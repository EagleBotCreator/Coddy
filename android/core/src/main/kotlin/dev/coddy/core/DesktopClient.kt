package dev.coddy.core
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.json.*

/** Sends only on an explicit call. No redirects, retries or provider fallback. */
class DesktopClient {
 private val active=AtomicReference<HttpURLConnection?>(null)
 fun cancel() { active.getAndSet(null)?.disconnect() }
 fun test(base: String, workspace: String, token: String) {
  val url=DesktopEndpoint.chatUrl(base,workspace).removeSuffix("/chat")
  request(url,token,null)
 }
 fun chat(base: String, workspace: String, token: String, messages: List<PromptMessage>, sessionId: String): String {
  val url=DesktopEndpoint.chatUrl(base,workspace)
  val message=messages.joinToString("\n\n"){ "[${it.role}]\n${it.content}" }
  val body=buildJsonObject {put("message",message);put("mode","chat");put("sessionId",sessionId)}.toString()
  val text=request(url,token,body)
  val json=try{Json.parseToJsonElement(text).jsonObject}catch(e:Exception){throw IllegalArgumentException("The computer returned an unreadable reply.")}
  require(json["error"]==null || json["error"]==JsonNull || json["error"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()){"The computer could not complete this request. Check its model and workspace."}
  val answer=json["textResponse"]?.jsonPrimitive?.contentOrNull
  require(!answer.isNullOrBlank()){"The computer returned an empty reply. Check its model and workspace."}
  return answer
 }
 private fun request(url: String, token: String, body: String?): String {
  require(token.isNotBlank() && token.length<=4096 && token.none{it.isISOControl()}){"Enter a valid desktop access token."}
  val connection=URI(url).toURL().openConnection() as HttpURLConnection
  connection.instanceFollowRedirects=false;connection.connectTimeout=10000;connection.readTimeout=120000
  connection.requestMethod=if(body==null)"GET" else "POST"
  connection.setRequestProperty("Authorization","Bearer $token")
  connection.setRequestProperty("Accept","application/json")
  active.set(connection)
  try {
   if(body!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}}
   val code=connection.responseCode
   require(code in 200..299){when(code){401,403->"The computer rejected the token. Check its access settings.";in 300..399->"The computer redirected the request. Enter its final address; redirects are blocked.";else->"The computer could not answer (HTTP $code). Check its workspace and model."}}
   val output=ByteArrayOutputStream()
   connection.inputStream.use {input->
    val buffer=ByteArray(8192)
    while(true){val size=input.read(buffer);if(size<0)break;require(output.size()+size<=1024*1024){"The computer's reply was too large."};output.write(buffer,0,size)}
   }
   return output.toString(Charsets.UTF_8.name())
  } catch(e:IllegalArgumentException){throw e}
    catch(e:Exception){throw IllegalArgumentException("Cannot reach the computer. Check Wi-Fi, address and that AnythingLLM is running.",e)}
    finally{active.compareAndSet(connection,null);connection.disconnect()}
 }
}
