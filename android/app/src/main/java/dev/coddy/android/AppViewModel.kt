package dev.coddy.android
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.coddy.android.data.*
import dev.coddy.android.inference.LocalEngine
import dev.coddy.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

data class CoddyUiState(
 val data:SavedData=SavedData(),val snapshot:MemorySnapshot?=null,val pendingSnapshot:MemorySnapshot?=null,
 val loaded:Boolean=false,val modelReady:Boolean=false,val busy:String?=null,val generating:Boolean=false,
 val partial:String="",val error:String?=null,val notice:String?=null,val storageBlocked:Boolean=false,
 val token:String="",val connectionStatus:String="Not tested",val memoryQuery:String="",
 val connectionForm:ConnectionSettings=ConnectionSettings(),val connectionDirty:Boolean=false
)
class AppViewModel(application:Application):AndroidViewModel(application){
 private val store=AppStore(application)
 private val secrets=TokenStore(application)
 private val local=LocalEngine()
 private val desktop=DesktopClient()
 private val state=MutableStateFlow(CoddyUiState())
 val ui:StateFlow<CoddyUiState> = state.asStateFlow()
 private var saveJob:Job?=null
 private var generation:Job?=null
 private var stopped=false
 private var savedToken=""
 private var savedDestination=""

 init{viewModelScope.launch{
  try{
   val data=withContext(Dispatchers.IO){store.load()}
   val snapshot=withContext(Dispatchers.IO){store.loadSnapshot()}
   val destination=runCatching{DesktopEndpoint.chatUrl(data.connection.baseUrl,data.connection.workspace)}.getOrDefault("")
   val token=withContext(Dispatchers.IO){runCatching{secrets.read(destination,data.connectionSecret)}.getOrDefault("")}
   savedToken=token
   savedDestination=destination
   state.value=CoddyUiState(data=data,snapshot=snapshot,token=token,loaded=true,connectionForm=data.connection)
   if(data.modelFile.isNotEmpty()){
    state.update{it.copy(busy="Loading your local model…")}
    try{local.load(store.modelFile(data.modelFile));state.update{it.copy(modelReady=true)}}catch(e:Exception){state.update{it.copy(error="Your local model could not be loaded. Choose the GGUF file again.")}}
    finally{state.update{it.copy(busy=null)}}
   }
  }catch(e:Exception){state.update{it.copy(loaded=true,storageBlocked=true,error="Saved data could not be opened. Nothing was overwritten. Keep your original files and see the recovery guide.")}}
 }}

 private fun change(transform:(SavedData)->SavedData){
  if(state.value.storageBlocked)return
  state.update{it.copy(data=transform(it.data).copy(revision=it.data.revision+1),notice=null)}
  persist()
 }
 private fun persist(){
  saveJob?.cancel()
  saveJob=viewModelScope.launch{
   delay(200)
   try{withContext(Dispatchers.IO){store.save(state.value.data)}}catch(e:Exception){state.update{it.copy(error="Could not save on this phone. Check free storage before closing the app.")}}
  }
 }
 fun flush(){
  if(!state.value.loaded||state.value.storageBlocked)return
  saveJob?.cancel()
  val commit=store.enqueueSave(state.value.data)
  viewModelScope.launch{try{commit.await()}catch(e:Exception){state.update{it.copy(error="Could not save on this phone. Check free storage.")}}}
 }
 fun chatDraft(value:String){
  if(value.length>16000){state.update{it.copy(error="This message is too long. Paste a shorter part (up to 4,000 characters).")};return}
  change{it.copy(chatDraft=value)}
 }
 fun draft(value:ProposalDraft){change{it.copy(proposalDraft=value)}}
 fun query(value:String){state.update{it.copy(memoryQuery=value)}}
 fun clearError(){state.update{it.copy(error=null)}}
 fun dismissNotice(){state.update{it.copy(notice=null)}}
 fun setProvider(provider:ChatProvider){
  if(state.value.generating)return
  if(provider==ChatProvider.DESKTOP){
   try{require(!state.value.connectionDirty&&savedToken.isNotBlank()){"Save a valid desktop connection first."};DesktopEndpoint.chatUrl(state.value.data.connection.baseUrl,state.value.data.connection.workspace)}
   catch(e:Exception){state.update{it.copy(error=e.message)};return}
  }
  change{it.copy(connection=it.connection.copy(provider=provider))}
  state.update{it.copy(connectionForm=it.connectionForm.copy(provider=provider))}
 }
 fun editConnection(url:String,workspace:String,token:String){
  if(state.value.busy!=null||state.value.generating||state.value.storageBlocked)return
  state.update{it.copy(connectionForm=it.connectionForm.copy(baseUrl=url.take(240),workspace=workspace.take(80)),token=token.take(4096),connectionStatus="Not tested",connectionDirty=true)}
 }
 fun saveConnection(){
  if(state.value.busy!=null||state.value.generating||state.value.storageBlocked)return
  viewModelScope.launch{
   state.update{it.copy(busy="Saving connection…",error=null)}
   try{
    val current=state.value
    val destination=DesktopEndpoint.chatUrl(current.connectionForm.baseUrl,current.connectionForm.workspace)
    require(current.token.isNotBlank()){"Enter the desktop access token."}
    val sealed=withContext(Dispatchers.IO){secrets.seal(destination,current.token)}
    savedToken=current.token
    savedDestination=destination
    change{it.copy(connection=current.connectionForm.copy(provider=it.connection.provider),connectionSecret=sealed)}
    withContext(Dispatchers.IO){store.save(state.value.data)}
    state.update{it.copy(connectionDirty=it.connectionForm!=current.connectionForm||it.token!=current.token,notice="Connection saved on this phone. The token is encrypted and bound to this address.")}
   }catch(e:Exception){state.update{it.copy(error=e.message?:"Could not save connection.")}}
   finally{state.update{it.copy(busy=null)}}
  }
 }
 fun testConnection(){
  if(state.value.busy!=null||state.value.generating)return
  viewModelScope.launch{
   state.update{it.copy(busy="Checking the computer…",error=null,connectionStatus="Checking…")}
   val current=state.value
   try{withContext(Dispatchers.IO){desktop.test(current.connectionForm.baseUrl,current.connectionForm.workspace,current.token)}
    state.update{it.copy(connectionStatus="Connection verified",notice="The computer accepted this workspace connection.")}
   }catch(e:Exception){state.update{it.copy(connectionStatus="Not connected",error=e.message?:"Cannot reach the computer.")}}
   finally{state.update{it.copy(busy=null)}}
  }
 }
 fun importModel(uri:Uri){
  if(state.value.busy!=null||state.value.generating||state.value.storageBlocked)return
  viewModelScope.launch{
   state.update{it.copy(busy="Copying the model…",error=null)}
   var copied:java.io.File?=null
   try{
    val (file,label)=withContext(Dispatchers.IO){store.copyModel(uri){bytes->state.update{it.copy(busy="Copying model · ${bytes/(1024*1024)} MiB")}}}
    copied=file
    state.update{it.copy(busy="Loading the model…")}
    local.load(file)
    copied=null
    state.update{it.copy(modelReady=true)}
    val previous=state.value.data.modelFile
    change{it.copy(modelFile=file.name,modelLabel=label)}
    withContext(Dispatchers.IO){store.save(state.value.data)}
    copied=null
    state.update{it.copy(modelReady=true,notice="Local model ready. You can disconnect from the internet.")}
    if(previous.isNotBlank()&&previous!=file.name)withContext(Dispatchers.IO){runCatching{store.modelFile(previous).delete()}}
   }catch(e:Exception){state.update{it.copy(error=e.message?:"Cannot load this model. Try a smaller instruction-tuned GGUF.")}}
   finally{copied?.delete();state.update{it.copy(busy=null)}}
  }
 }
 fun stageSnapshot(uri:Uri){
  if(state.value.busy!=null||state.value.storageBlocked)return
  viewModelScope.launch{
   state.update{it.copy(busy="Reading the snapshot…",error=null)}
   try{val snapshot=withContext(Dispatchers.IO){store.readSnapshot(uri)};state.update{it.copy(pendingSnapshot=snapshot)}}
   catch(e:Exception){state.update{it.copy(error=e.message?:"Cannot import this snapshot. Your current snapshot is unchanged.")}}
   finally{state.update{it.copy(busy=null)}}
  }
 }
 fun dismissSnapshot(){state.update{it.copy(pendingSnapshot=null)}}
 fun confirmSnapshot(){
  val snapshot=state.value.pendingSnapshot?:return
  if(state.value.busy!=null)return
  viewModelScope.launch{
   state.update{it.copy(busy="Saving the snapshot…")}
   try{withContext(Dispatchers.IO){store.saveSnapshot(snapshot)};state.update{it.copy(snapshot=snapshot,pendingSnapshot=null,memoryQuery="",notice="${snapshot.notes.size} read-only memories imported.")}}
   catch(e:Exception){state.update{it.copy(error="Cannot save the snapshot. Your previous snapshot is unchanged.")}}
   finally{state.update{it.copy(busy=null)}}
  }
 }
 fun saveProposal():Boolean{
  if(state.value.storageBlocked)return false
  try{
   val draft=state.value.data.proposalDraft
   val proposal=MemoryProposal(id(),draft.category,draft.title.trim(),draft.content.trim(),draft.source.trim(),draft.importance,draft.confidence,now())
   ProposalWorkflow.validate(proposal)
   require(state.value.data.proposals.size<2000){"Export and review existing proposals before adding more."}
   require(state.value.data.proposals.none{it.state!=ProposalState.REJECTED&&it.title==proposal.title&&it.content==proposal.content}){"This proposal is already in your queue."}
   change{it.copy(proposals=it.proposals+proposal,proposalDraft=ProposalDraft())}
   state.update{it.copy(notice="Proposal saved for review. It has not been added to memory.")}
   return true
  }catch(e:Exception){state.update{it.copy(error=e.message?:"Check the proposal fields.")};return false}
 }
 fun reviewProposal(id:String){
  try{change{data->data.copy(proposals=data.proposals.map{if(it.id==id)ProposalWorkflow.review(it)else it})};state.update{it.copy(notice="Reviewed for export. Desktop approval is still required.")}}
  catch(e:Exception){state.update{it.copy(error=e.message)}}
 }
 fun rejectProposal(id:String){change{data->data.copy(proposals=data.proposals.map{if(it.id==id)ProposalWorkflow.reject(it)else it})}}
 fun exportProposals(uri:Uri){
  viewModelScope.launch{
   try{val text=ProposalWorkflow.export(state.value.data.proposals,now());withContext(Dispatchers.IO){store.writeExport(uri,text)};state.update{it.copy(notice="Reviewed proposals exported. Import them into the desktop approval queue.")}}
   catch(e:Exception){state.update{it.copy(error="Could not write the export. Choose a location with free space and try again.")}}
  }
 }
 fun send(){
  val current=state.value
  if(current.generating||current.busy!=null||current.storageBlocked||current.data.chatDraft.isBlank())return
  if(current.data.chatDraft.length>4000){state.update{it.copy(error="Shorten this message to 4,000 characters. Nothing was sent.")};return}
  if(current.data.connection.provider==ChatProvider.LOCAL&&!current.modelReady){state.update{it.copy(error="Choose a local model in Connect before sending.")};return}
  val requestToken=savedToken
  if(current.data.connection.provider==ChatProvider.DESKTOP){
   val destination=runCatching{DesktopEndpoint.chatUrl(current.data.connection.baseUrl,current.data.connection.workspace)}.getOrDefault("")
   if(destination!=savedDestination||requestToken.isBlank()){state.update{it.copy(error="Save this desktop connection and token again before sending.")};return}
  }
  val user=ChatMessage(id(),"user",current.data.chatDraft.trim(),now())
  val history=(current.data.messages+user).takeLast(200)
  val notes=MemorySearch.search(current.snapshot?.notes.orEmpty(),user.content,3)
  val prompt=PromptBuilder.build(history,notes)
  val ticket=local.ticket()
  change{it.copy(messages=history,chatDraft="")}
  state.update{it.copy(generating=true,partial="",error=null)}
  stopped=false
  generation=viewModelScope.launch{
   try{
    val answer=if(current.data.connection.provider==ChatProvider.LOCAL){
     var last=0L
     local.generate(prompt,ticket){text->
      val tick=System.nanoTime()
      if(tick-last>60_000_000){last=tick;state.update{it.copy(partial=text)}}
     }
    }else withContext(Dispatchers.IO){desktop.chat(current.data.connection.baseUrl,current.data.connection.workspace,requestToken,prompt,current.data.sessionId)}
    if(answer.isNotBlank())appendAnswer(answer,notes,false)
    else if(!stopped)state.update{it.copy(error="The model returned no text. Try another prompt or an instruction-tuned model.")}
   }catch(e:CancellationException){
    val partial=state.value.partial
    if(partial.isNotBlank())appendAnswer(partial,notes,true)
    throw e
   }catch(e:Exception){
    val partial=state.value.partial
    if(partial.isNotBlank())appendAnswer(partial,notes,true)
    state.update{it.copy(error=e.message?:"The reply could not be completed. Your message is saved.")}
   }finally{state.update{it.copy(generating=false,partial="")};persist()}
  }
 }
 private fun appendAnswer(text:String,notes:List<MemoryNote>,isStopped:Boolean){change{it.copy(messages=(it.messages+ChatMessage(id(),"assistant",text,now(),notes.map{n->n.source},isStopped)).takeLast(200))}}
 fun stop(){stopped=true;local.stop();desktop.cancel();generation?.cancel()}
 fun clearChat(){if(state.value.generating)return;change{it.copy(messages=emptyList(),sessionId=UUID.randomUUID().toString(),chatDraft="")}}
 override fun onCleared(){flush();local.stop();desktop.cancel();CoroutineScope(Dispatchers.IO).launch{local.close()};super.onCleared()}
 private fun id()=UUID.randomUUID().toString().replace("-","")
 private fun now()=Instant.now().toString()
}
