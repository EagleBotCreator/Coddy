package dev.coddy.core
import java.net.URI
object DesktopEndpoint {
 fun chatUrl(baseUrl: String, workspace: String): String {
  require(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,79}").matches(workspace)){"Use the workspace slug, such as coddy."}
  val uri=try { URI(baseUrl.trim()) } catch(e:Exception){throw IllegalArgumentException("Enter a valid computer address.",e)}
  require(uri.scheme in setOf("http","https") && !uri.host.isNullOrBlank() && uri.rawUserInfo==null && uri.query==null && uri.fragment==null && uri.path in listOf("","/") && uri.port in -1..65535 && uri.port!=0){"Use a plain HTTP(S) address without a path, credentials or query."}
  if(uri.scheme=="http") {
   val host=uri.host
   val parts=host.split('.')
   val ipv4=parts.size==4 && parts.all{Regex("0|[1-9][0-9]{0,2}").matches(it) && it.toInt()<=255}
   val local=if(ipv4){val oct=parts.map{it.toInt()};oct[0]==10||oct[0]==127||(oct[0]==192&&oct[1]==168)||(oct[0]==172&&oct[1] in 16..31)}else host=="[::1]"||host=="::1"
   require(local){"For HTTP, use a private LAN IP address. Other hosts require HTTPS."}
  }
  return "${uri.scheme}://${uri.rawAuthority}/api/v1/workspace/$workspace/chat"
 }
}
