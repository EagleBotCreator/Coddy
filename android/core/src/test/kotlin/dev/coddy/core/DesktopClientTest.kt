package dev.coddy.core
import kotlin.test.*
import org.junit.Test
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
class DesktopClientTest {
 @Test fun sendsTokenOnlyToChosenHostAndParsesText() {
  MockWebServer().use { server ->
   server.enqueue(MockResponse().setBody("""{"textResponse":"Hello Matt","error":null}"""))
   server.start()
   val client=DesktopClient()
   assertEquals("Hello Matt",client.chat(server.url("/").toString().replace("localhost", "127.0.0.1"),"coddy","test-token",listOf(PromptMessage("user","Hello")),"test-session"))
   val request=server.takeRequest()
   assertEquals("Bearer test-token",request.getHeader("Authorization"))
   assertEquals("/api/v1/workspace/coddy/chat",request.path)
   assertTrue(request.body.readUtf8().contains("Hello"))
  }
 }
 @Test fun redirectIsNotFollowedAndSecretDoesNotAppearInErrors() {
  MockWebServer().use{server->
   server.enqueue(MockResponse().setResponseCode(302).addHeader("Location","https://example.com/stolen"))
   server.start()
   val error=assertFailsWith<IllegalArgumentException>{DesktopClient().chat(server.url("/").toString().replace("localhost", "127.0.0.1"),"coddy","secret-token",listOf(PromptMessage("user","Hello")),"session")}
   assertFalse(error.message.orEmpty().contains("secret-token"))
   assertEquals(1,server.requestCount)
  }
 }
 @Test fun serverErrorDoesNotBecomeAnAssistantReply() {
  MockWebServer().use{server->
   server.enqueue(MockResponse().setResponseCode(401).setBody("bad token"))
   server.start()
   assertFailsWith<IllegalArgumentException>{DesktopClient().chat(server.url("/").toString().replace("localhost", "127.0.0.1"),"coddy","secret",emptyList(),"session")}
  }
 }
}
