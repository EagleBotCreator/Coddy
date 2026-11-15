package dev.coddy.android.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.coddy.android.*
import dev.coddy.core.*
import kotlinx.coroutines.launch

@Composable fun CoddyApp(vm:AppViewModel){
 val ui by vm.ui.collectAsStateWithLifecycle()
 var tab by rememberSaveable{mutableIntStateOf(0)}
 var proposalOpen by rememberSaveable{mutableStateOf(false)}
 var clearChatOpen by rememberSaveable{mutableStateOf(false)}
 var selectedNote by rememberSaveable{mutableStateOf<String?>(null)}
 var selectedProposal by rememberSaveable{mutableStateOf<String?>(null)}
 val snackbar=remember{SnackbarHostState()}
 val snapshotPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::stageSnapshot)}
 val modelPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::importModel)}
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(vm::exportProposals)}
 LaunchedEffect(ui.notice){ui.notice?.let{snackbar.showSnackbar(it);vm.dismissNotice()}}
 val keyboard=WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)>0
 Scaffold(
  modifier=Modifier.imePadding(),
  containerColor=CoddyColors.Background,
  topBar={Column(Modifier.statusBarsPadding().fillMaxWidth()){
   Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){
    Surface(shape=RoundedCornerShape(14.dp),color=CoddyColors.Primary){
     Text("c",Modifier.padding(horizontal=15.dp,vertical=6.dp),color=CoddyColors.Surface,style=MaterialTheme.typography.headlineMedium)
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)){Text("Coddy",style=MaterialTheme.typography.titleLarge);Text("A little space to think",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)}
    val local=ui.data.connection.provider==ChatProvider.LOCAL
    StatusChip(if(local)"This phone" else "Desktop",if(local&&ui.modelReady)CoddyColors.Success else CoddyColors.Muted)
   }
   if(ui.busy!=null)LinearProgressIndicator(Modifier.fillMaxWidth(),color=CoddyColors.Secondary)
  }},
  bottomBar={if(!keyboard)NavigationBar(containerColor=CoddyColors.Surface){
   listOf("Chat" to Icons.Outlined.Forum,"Memory" to Icons.Outlined.Description,"Connect" to Icons.Outlined.Tune).forEachIndexed{i,item->
    NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(item.second,null)},label={Text(item.first)},colors=NavigationBarItemDefaults.colors(indicatorColor=CoddyColors.Primary.copy(alpha=.08f)))
   }
  }},
  snackbarHost={SnackbarHost(snackbar)}
 ){padding->
  Column(Modifier.padding(padding).fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){
   if(ui.busy!=null)Text(ui.busy!!,Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=6.dp),style=MaterialTheme.typography.bodySmall)
   if(ui.error!=null&&!proposalOpen)Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),shape=RoundedCornerShape(16.dp),color=CoddyColors.Danger.copy(alpha=.08f)){
    Row(Modifier.padding(start=16.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){
     Text(ui.error!!,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=CoddyColors.Danger)
     if(!ui.storageBlocked)IconButton(onClick=vm::clearError){Icon(Icons.Outlined.Close,"Dismiss error",tint=CoddyColors.Danger)}
    }
   }
   Box(Modifier.weight(1f).widthIn(max=720.dp).fillMaxWidth()){
    if(!ui.loaded)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
    else when(tab){
     0->ChatScreen(ui,vm,onConnect={tab=2},onMemory={tab=1},onProposal={proposalOpen=true},onClear={clearChatOpen=true})
     1->MemoryScreen(ui,vm,onImport={snapshotPicker.launch(arrayOf("application/json","text/plain","application/octet-stream"))},onNote={selectedNote=it},onProposal={proposalOpen=true},onReview={selectedProposal=it},onExport={exporter.launch("coddy-reviewed-proposals.json")})
     else->ConnectScreen(ui,vm,onModel={modelPicker.launch(arrayOf("*/*"))})
    }
   }
  }
 }
 if(clearChatOpen)AlertDialog(onDismissRequest={clearChatOpen=false},title={Text("Clear this chat?")},text={Text("This removes the conversation stored on this phone. Your memory snapshot and proposals stay.")},dismissButton={TextButton(onClick={clearChatOpen=false}){Text("Keep chat")}},confirmButton={TextButton(onClick={vm.clearChat();clearChatOpen=false},colors=ButtonDefaults.textButtonColors(contentColor=CoddyColors.Danger)){Text("Clear chat")}})
 if(ui.pendingSnapshot!=null){
  val snapshot=ui.pendingSnapshot!!
  AlertDialog(onDismissRequest=vm::dismissSnapshot,title={Text("Import this snapshot?")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("${snapshot.notes.size} approved memories");Text("Exported ${dateLabel(snapshot.exported_at)}",style=MaterialTheme.typography.bodyMedium);Text("This replaces the read-only snapshot on this phone. Your original file and Obsidian vault stay unchanged.")}},dismissButton={TextButton(onClick=vm::dismissSnapshot,enabled=ui.busy==null){Text("Cancel")}},confirmButton={Button(onClick=vm::confirmSnapshot,enabled=ui.busy==null){Text("Import memories")}})
 }
 selectedNote?.let{id->ui.snapshot?.notes?.find{it.id==id}?.let{note->
  AlertDialog(onDismissRequest={selectedNote=null},title={Text(note.title)},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){StatusChip(note.category);Text(note.content,style=MaterialTheme.typography.bodyLarge);Provenance(note.source);Text("Created ${dateLabel(note.created_at)}",style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton(onClick={selectedNote=null}){Text("Close")}})
 }}
 if(proposalOpen)ProposalEditor(ui,vm,onClose={proposalOpen=false})
 selectedProposal?.let{id->ui.data.proposals.find{it.id==id}?.let{p->
  AlertDialog(onDismissRequest={selectedProposal=null},title={Text("Review this proposal")},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(p.title,style=MaterialTheme.typography.titleLarge);Text(p.content);Provenance(p.source,"Proposed source");Text("Importance ${p.importance}/5 · Confidence ${(p.confidence*100).toInt()}%");Text("Reviewing makes this eligible for export. It does not save anything to Obsidian.",color=CoddyColors.Muted)}},dismissButton={TextButton(onClick={vm.rejectProposal(id);selectedProposal=null}){Text("Reject")}},confirmButton={Button(onClick={vm.reviewProposal(id);selectedProposal=null}){Text("Mark reviewed")}})
 }}
}
