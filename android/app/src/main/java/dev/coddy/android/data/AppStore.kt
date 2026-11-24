package dev.coddy.android.data
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import dev.coddy.core.*
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.FileNotFoundException
import kotlinx.coroutines.Deferred

@Serializable data class ProposalDraft(val category:String="fact",val title:String="",val content:String="",val source:String="Written on Android",val importance:Int=3,val confidence:Double=0.9)
@Serializable data class SavedData(
 val version:Int=1,val revision:Long=0,val messages:List<ChatMessage> = emptyList(),val proposals:List<MemoryProposal> = emptyList(),
 val connection:ConnectionSettings=ConnectionSettings(),val connectionSecret:String="",val chatDraft:String="",val proposalDraft:ProposalDraft=ProposalDraft(),
 val modelFile:String="",val modelLabel:String="",val sessionId:String=UUID.randomUUID().toString()
)
class AppStore(private val context:Context) {
 private val file=AtomicFile(File(context.filesDir,"state.json"))
 private val snapshotFile=AtomicFile(File(context.filesDir,"snapshot.json"))
 suspend fun load():SavedData = StoreWriter.submit {
  val text=readRecoverable(file)
  val state=if(text==null)SavedData() else CoddyJson.decodeFromString<SavedData>(text)
  require(state.version==1){"Unsupported saved app data."}
  state.proposals.forEach{ProposalWorkflow.validate(it)}
  state
 }.await()
 suspend fun loadSnapshot():MemorySnapshot? = StoreWriter.submit {readRecoverable(snapshotFile)?.let(SnapshotCodec::decode)}.await()
 fun enqueueSave(data:SavedData):Deferred<Unit> = StoreWriter.writeLatest(file.baseFile.path,data.revision){atomicWrite(file,CoddyJson.encodeToString(data))}
 suspend fun save(data:SavedData){enqueueSave(data).await()}
 suspend fun saveSnapshot(snapshot:MemorySnapshot){StoreWriter.submit{atomicWrite(snapshotFile,CoddyJson.encodeToString(snapshot))}.await()}
 private fun readRecoverable(target:AtomicFile):String?{
  return try{target.openRead().use{it.bufferedReader().readText()}}
  catch(e:FileNotFoundException){
   val path=target.baseFile
   if(path.exists()||File(path.path+".bak").exists()||File(path.path+".new").exists())throw e
   null
  }
 }
 private fun atomicWrite(target:AtomicFile,text:String){
  val stream=target.startWrite()
  try{stream.write(text.toByteArray(Charsets.UTF_8));target.finishWrite(stream)}catch(e:Exception){target.failWrite(stream);throw e}
 }
 fun modelFile(name:String):File {
  require(Regex("[a-f0-9]{32}\\.gguf").matches(name)){"Invalid saved model name."}
  return File(File(context.filesDir,"models"),name)
 }
 fun readSnapshot(uri:Uri):MemorySnapshot {
  val bytes=ByteArrayOutputStream()
  context.contentResolver.openInputStream(uri)?.use {input->
   val buffer=ByteArray(8192)
   while(true){val n=input.read(buffer);if(n<0)break;require(bytes.size()+n<=MAX_SNAPSHOT_BYTES){"Snapshot exceeds 8 MiB."};bytes.write(buffer,0,n)}
  } ?: throw IllegalArgumentException("Cannot open this file. Choose it again.")
  return SnapshotCodec.decode(bytes.toString(Charsets.UTF_8.name()))
 }
 fun copyModel(uri:Uri,onProgress:(Long)->Unit):Pair<File,String>{
  val dir=File(context.filesDir,"models").apply{mkdirs()}
  val target=File(dir,UUID.randomUUID().toString().replace("-","")+".gguf")
  var label="Local model.gguf"
  context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {cursor->if(cursor.moveToFirst()) label=cursor.getString(0).take(120)}
  try{
   val input=context.contentResolver.openInputStream(uri)?:throw IllegalArgumentException("Cannot open this model. Choose the file again.")
   input.use{source->
    val magic=ByteArray(4);var count=0
    while(count<4){val read=source.read(magic,count,4-count);require(read>0){"This is not a GGUF model."};count+=read}
    require(magic.contentEquals(byteArrayOf(0x47,0x47,0x55,0x46))){"Choose a GGUF model file, not a ZIP or webpage."}
    target.outputStream().use{output->
     output.write(magic);val buffer=ByteArray(1024*1024);var total=4L
     while(true){val read=source.read(buffer);if(read<0)break;total+=read;require(total<=4L*1024*1024*1024){"Use a GGUF under 4 GiB."};require(dir.usableSpace>read+32L*1024*1024){"Not enough storage. Free space and choose the model again."};output.write(buffer,0,read);onProgress(total)}
    }
   }
   return target to label
  }catch(e:Throwable){target.delete();throw e}
 }
 fun writeExport(uri:Uri,text:String) {
  val output=context.contentResolver.openOutputStream(uri,"wt")?:throw IllegalArgumentException("Cannot create the export file. Choose another location.")
  output.use{it.write(text.toByteArray(Charsets.UTF_8))}
 }
}
