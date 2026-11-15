package dev.coddy.android.inference
import androidx.annotation.Keep
import dev.coddy.core.PromptMessage
import java.io.File
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Keep object NativeBridge {
 init {System.loadLibrary("coddy")}
 external fun loadModel(path: String): Long
 external fun unloadModel(handle: Long)
 external fun ticket(): Long
 external fun stop()
 external fun generate(handle: Long, roles: Array<ByteArray>, contents: Array<ByteArray>, ticket: Long, sink: TokenSink): ByteArray
}
@Keep class TokenSink(private val callback: (ByteArray)->Unit) { @Keep fun onToken(bytes: ByteArray) = callback(bytes) }
class LocalEngine {
 private val mutex=Mutex()
 private var handle=0L
 suspend fun load(file: File) = withContext(Dispatchers.IO) {
  mutex.withLock {
   val next=NativeBridge.loadModel(file.absolutePath)
   check(next!=0L){"Model failed to load."}
   val previous=handle;handle=next
   if(previous!=0L)NativeBridge.unloadModel(previous)
  }
 }
 fun ticket()=NativeBridge.ticket()
 fun stop()=NativeBridge.stop()
 suspend fun generate(messages: List<PromptMessage>, ticket: Long, onText: (String)->Unit): String = withContext(Dispatchers.IO) {
  mutex.withLock {
   check(handle!=0L){"Choose and load a model first."}
   val output=ByteArrayOutputStream()
   val sink=TokenSink {bytes->output.write(bytes);onText(output.toString(Charsets.UTF_8.name()))}
   NativeBridge.generate(handle,messages.map{it.role.toByteArray()}.toTypedArray(),messages.map{it.content.toByteArray()}.toTypedArray(),ticket,sink).toString(Charsets.UTF_8)
  }
 }
 suspend fun close()=withContext(Dispatchers.IO){mutex.withLock{if(handle!=0L){NativeBridge.unloadModel(handle);handle=0L}}}
}
