package dev.coddy.android
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.coddy.android.inference.LocalEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativeRuntimeTest{
 @Test fun nativeLibraryLoadsAndInvalidModelFailsWithoutCrashing()=runBlocking{
  val engine=LocalEngine()
  engine.ticket()
  try{engine.load(File("/does-not-exist.gguf"));fail("Invalid model must fail")}catch(e:IllegalStateException){assertTrue(e.message.orEmpty().contains("GGUF"))}
  engine.stop();engine.close()
 }
}
