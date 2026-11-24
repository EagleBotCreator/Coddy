package dev.coddy.android.data
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Process-lifetime serialized I/O. Finishing an Activity cannot cancel queued commits. */
internal object StoreWriter {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val queue=Channel<()->Unit>(Channel.UNLIMITED)
 private val revisions=mutableMapOf<String,Long>()
 init{scope.launch{for(task in queue)task()}}
 fun <T> submit(block:()->T):Deferred<T>{
  val result=CompletableDeferred<T>()
  check(queue.trySend{try{result.complete(block())}catch(e:Throwable){result.completeExceptionally(e)}}.isSuccess)
  return result
 }
 fun writeLatest(path:String,revision:Long,block:()->Unit):Deferred<Unit> = submit{
  if(revision>=(revisions[path]?:Long.MIN_VALUE)){block();revisions[path]=revision}
 }
}
