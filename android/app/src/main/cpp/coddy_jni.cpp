#include <jni.h>
#include <llama.h>
#include <atomic>
#include <algorithm>
#include <cstdint>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace {
std::atomic<int64_t> epoch{0};
std::once_flag backend_once;
struct Engine {
 llama_model * model = nullptr;
 llama_context * context = nullptr;
 ~Engine(){if(context)llama_free(context);if(model)llama_model_free(model);}
};
struct AbortState {int64_t ticket;};
bool aborted(void * data){return epoch.load()!=static_cast<AbortState*>(data)->ticket;}
void fail(JNIEnv * env,const char * message){env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),message);}
std::string bytes(JNIEnv * env,jbyteArray value){
 auto count=env->GetArrayLength(value);std::string output(count,'\0');
 env->GetByteArrayRegion(value,0,count,reinterpret_cast<jbyte*>(output.data()));return output;
}
jbyteArray byte_array(JNIEnv * env,const std::string & value){
 auto result=env->NewByteArray(static_cast<jsize>(value.size()));
 env->SetByteArrayRegion(result,0,static_cast<jsize>(value.size()),reinterpret_cast<const jbyte*>(value.data()));return result;
}
}

extern "C" JNIEXPORT jlong JNICALL Java_dev_coddy_android_inference_NativeBridge_loadModel(JNIEnv * env,jobject,jstring path){
 try {
  std::call_once(backend_once,[]{llama_log_set([](ggml_log_level,const char*,void*){},nullptr);llama_backend_init();});
  const char * raw=env->GetStringUTFChars(path,nullptr);
  std::string filename(raw);env->ReleaseStringUTFChars(path,raw);
  auto state=std::make_unique<Engine>();
  auto params=llama_model_default_params();params.n_gpu_layers=0;params.load_mode=LLAMA_LOAD_MODE_MMAP;
  state->model=llama_model_load_from_file(filename.c_str(),params);
  if(!state->model)throw std::runtime_error("Cannot load this model. Choose a supported instruction-tuned GGUF and check available RAM.");
  auto context_params=llama_context_default_params();context_params.n_ctx=2048;context_params.n_batch=512;context_params.n_ubatch=128;
  context_params.n_threads=std::min(4u,std::max(1u,std::thread::hardware_concurrency()));
  context_params.n_threads_batch=context_params.n_threads;
  state->context=llama_init_from_model(state->model,context_params);
  if(!state->context)throw std::runtime_error("Not enough memory for this model. Try a smaller quantized GGUF.");
  return reinterpret_cast<jlong>(state.release());
 } catch(const std::exception & e){fail(env,e.what());return 0;}
}

extern "C" JNIEXPORT void JNICALL Java_dev_coddy_android_inference_NativeBridge_unloadModel(JNIEnv*,jobject,jlong pointer){delete reinterpret_cast<Engine*>(pointer);}
extern "C" JNIEXPORT jlong JNICALL Java_dev_coddy_android_inference_NativeBridge_ticket(JNIEnv*,jobject){return epoch.load();}
extern "C" JNIEXPORT void JNICALL Java_dev_coddy_android_inference_NativeBridge_stop(JNIEnv*,jobject){epoch.fetch_add(1);}

extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_coddy_android_inference_NativeBridge_generate(JNIEnv * env,jobject,jlong pointer,jobjectArray roles,jobjectArray contents,jlong ticket,jobject sink){
 try {
  auto * state=reinterpret_cast<Engine*>(pointer);
  if(!state)throw std::runtime_error("Choose and load a model first.");
  AbortState abort{ticket};
  if(aborted(&abort))return byte_array(env,"");
  const int count=env->GetArrayLength(roles);
  if(count!=env->GetArrayLength(contents)||count>20)throw std::runtime_error("Invalid conversation.");
  std::vector<std::string> role_storage,content_storage;
  role_storage.reserve(count);content_storage.reserve(count);
  for(int i=0;i<count;i++){
   auto role=static_cast<jbyteArray>(env->GetObjectArrayElement(roles,i));
   auto content=static_cast<jbyteArray>(env->GetObjectArrayElement(contents,i));
   role_storage.push_back(bytes(env,role));content_storage.push_back(bytes(env,content));
   env->DeleteLocalRef(role);env->DeleteLocalRef(content);
  }
  std::vector<llama_chat_message> messages;
  for(int i=0;i<count;i++)messages.push_back({role_storage[i].c_str(),content_storage[i].c_str()});
  const char * tmpl=llama_model_chat_template(state->model,nullptr);
  if(!tmpl)tmpl="chatml";
  int needed=llama_chat_apply_template(tmpl,messages.data(),messages.size(),true,nullptr,0);
  if(needed<0)throw std::runtime_error("This model's chat template is not supported. Choose a ChatML-compatible instruction model.");
  std::vector<char> prompt(needed+1);
  llama_chat_apply_template(tmpl,messages.data(),messages.size(),true,prompt.data(),prompt.size());
  auto * vocab=llama_model_get_vocab(state->model);
  int tokens_needed=llama_tokenize(vocab,prompt.data(),needed,nullptr,0,true,true);
  if(tokens_needed>=0)throw std::runtime_error("The model could not tokenize this message.");
  tokens_needed=-tokens_needed;
  constexpr int max_output=384;
  if(tokens_needed>static_cast<int>(llama_n_ctx(state->context))-max_output)
   throw std::runtime_error("This conversation is too long for the local model. Start a new chat or shorten your message.");
  std::vector<llama_token> tokens(tokens_needed);
  if(llama_tokenize(vocab,prompt.data(),needed,tokens.data(),tokens.size(),true,true)<0)throw std::runtime_error("Cannot process this message.");
  llama_memory_clear(llama_get_memory(state->context),true);
  llama_set_abort_callback(state->context,aborted,&abort);
  struct ResetAbort{llama_context * ctx;~ResetAbort(){llama_set_abort_callback(ctx,nullptr,nullptr);}} reset{state->context};
  for(int offset=0;offset<tokens_needed;offset+=512){
   if(aborted(&abort))return byte_array(env,"");
   auto batch=llama_batch_get_one(tokens.data()+offset,std::min(512,tokens_needed-offset));
   if(llama_decode(state->context,batch)!=0){if(aborted(&abort))return byte_array(env,"");throw std::runtime_error("Model processing failed. Try a shorter message or a smaller model.");}
  }
  std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_chain_init(llama_sampler_chain_default_params()),llama_sampler_free);
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_k(40));
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_p(0.9f,1));
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_temp(0.65f));
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
  auto sink_class=env->GetObjectClass(sink);
  auto on_token=env->GetMethodID(sink_class,"onToken","([B)V");
  if(!on_token)throw std::runtime_error("Cannot initialize token streaming.");
  std::string output;
  for(int step=0;step<max_output&&!aborted(&abort);step++){
   auto token=llama_sampler_sample(sampler.get(),state->context,-1);
   if(llama_vocab_is_eog(vocab,token))break;
   std::vector<char> piece(256);
   auto length=llama_token_to_piece(vocab,token,piece.data(),piece.size(),0,false);
   if(length<0){piece.resize(-length);length=llama_token_to_piece(vocab,token,piece.data(),piece.size(),0,false);}
   if(length>0){
    output.append(piece.data(),length);
    auto chunk=byte_array(env,std::string(piece.data(),length));
    env->CallVoidMethod(sink,on_token,chunk);env->DeleteLocalRef(chunk);
    if(env->ExceptionCheck())return nullptr;
   }
   auto batch=llama_batch_get_one(&token,1);
   if(llama_decode(state->context,batch)!=0){if(aborted(&abort))break;throw std::runtime_error("Generation stopped. Start a new chat or try a smaller model.");}
  }
  return byte_array(env,output);
 } catch(const std::exception & e){fail(env,e.what());return nullptr;}
}
