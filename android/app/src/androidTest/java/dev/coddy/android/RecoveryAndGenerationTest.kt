package dev.coddy.android
import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import dev.coddy.android.data.AppStore
import dev.coddy.android.data.SavedData
import dev.coddy.android.inference.LocalEngine
import dev.coddy.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID
class RecoveryAndGenerationTest{
 @Test fun backupOnlyStateAndSnapshotAreRecovered()=runBlocking{
  val target=InstrumentationRegistry.getInstrumentation().targetContext
  val dir=File(target.cacheDir,"recovery-"+UUID.randomUUID()).apply{mkdirs()}
  val context=object:ContextWrapper(target){override fun getFilesDir()=dir}
  try{
   File(dir,"state.json.bak").writeText(CoddyJson.encodeToString(SavedData(chatDraft="Retain this draft")))
   assertEquals("Retain this draft",AppStore(context).load().chatDraft)
   val snapshot=MemorySnapshot(exported_at="2026-11-15T16:00:00Z",notes=emptyList())
   File(dir,"snapshot.json.bak").writeText(CoddyJson.encodeToString(snapshot))
   assertEquals(snapshot,AppStore(context).loadSnapshot())
  }finally{dir.deleteRecursively()}
 }
 @Test fun externallyProvidedTinyModelGeneratesTokens()=runBlocking{
  val path=InstrumentationRegistry.getArguments().getString("ggufPath")
  assumeTrue("No external smoke model supplied",!path.isNullOrBlank())
  val engine=LocalEngine()
  try{
   engine.load(File(path!!))
   var updates=0
   val answer=engine.generate(listOf(PromptMessage("user","Once upon a time")),engine.ticket()){updates++}
   assertTrue("Native inference must produce tokens",updates>0)
   assertTrue("Native inference must return text",answer.isNotBlank())
  }finally{engine.close()}
 }
}
