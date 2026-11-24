package dev.coddy.android
import androidx.test.platform.app.InstrumentationRegistry
import dev.coddy.android.data.TokenStore
import org.junit.Assert.*
import org.junit.Test
class TokenBindingTest{
 @Test fun encryptedTokenCannotBeOpenedForAnotherComputer(){
  val store=TokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
  val token="synthetic-test-token"
  val payload=store.seal("https://computer-a.example/api/v1/workspace/coddy/chat",token)
  assertFalse(payload.contains(token))
  assertEquals(token,store.read("https://computer-a.example/api/v1/workspace/coddy/chat",payload))
  try{store.read("https://computer-b.example/api/v1/workspace/coddy/chat",payload);fail("Destination mismatch must reject the token")}catch(e:javax.crypto.AEADBadTagException){/* expected */}
 }
}
