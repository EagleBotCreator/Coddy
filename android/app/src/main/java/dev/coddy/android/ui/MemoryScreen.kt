package dev.coddy.android.ui
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
import androidx.compose.ui.unit.dp
import dev.coddy.android.*
import dev.coddy.core.*

@Composable fun MemoryScreen(ui:CoddyUiState,vm:AppViewModel,onImport:()->Unit,onNote:(String)->Unit,onProposal:()->Unit,onReview:(String)->Unit,onExport:()->Unit){
 var proposals by rememberSaveable{mutableStateOf(false)}
 val notes=remember(ui.snapshot,ui.memoryQuery){MemorySearch.search(ui.snapshot?.notes.orEmpty(),ui.memoryQuery,2000)}
 val reviewed=ui.data.proposals.count{it.state==ProposalState.REVIEWED}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{
   Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text("Your memory",style=MaterialTheme.typography.headlineLarge)
    Text("Context you can see. Changes you control.",color=CoddyColors.Muted)
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
     FilterChip(selected=!proposals,onClick={proposals=false},label={Text("Snapshot")},modifier=Modifier.heightIn(min=48.dp))
     FilterChip(selected=proposals,onClick={proposals=true},label={Text("Proposals · ${ui.data.proposals.count{it.state!=ProposalState.REJECTED}}")},modifier=Modifier.heightIn(min=48.dp))
    }
   }
  }
  if(!proposals){
   item{
    SectionCard{
     StatusChip("Read-only snapshot",CoddyColors.Accent)
     Text(ui.snapshot?.let{"${it.notes.size} memories · exported ${dateLabel(it.exported_at)}"}?:"No snapshot imported yet",style=MaterialTheme.typography.bodyMedium)
     OutlinedButton(onClick=onImport,enabled=ui.busy==null&&!ui.storageBlocked,modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Outlined.FileOpen,null);Spacer(Modifier.width(8.dp));Text("Import snapshot")}
    }
   }
   if(ui.snapshot!=null)item{
    OutlinedTextField(value=ui.memoryQuery,onValueChange=vm::query,label={Text("Search memory")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(ui.memoryQuery.isNotEmpty())IconButton(onClick={vm.query("")}){Icon(Icons.Outlined.Close,"Clear search")}},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(16.dp))
   }
   if(notes.isEmpty())item{EmptyPanel(Icons.Outlined.Description,if(ui.snapshot==null)"Bring your context along" else if(ui.memoryQuery.isBlank())"This snapshot is empty" else "No matching memories",if(ui.snapshot==null)"Export selected approved notes from Coddy on Windows, then import the JSON file here." else "Try fewer words or clear the search.",if(ui.memoryQuery.isNotBlank())"Clear search" else null,{vm.query("")})}
   items(notes,key={it.id}){note->
    Surface(onClick={onNote(note.id)},shape=RoundedCornerShape(24.dp),color=CoddyColors.Surface,modifier=Modifier.fillMaxWidth()){
     Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      SectionLabel(note.category.uppercase());Text(note.title,style=MaterialTheme.typography.titleLarge)
      Text(note.content.take(180),style=MaterialTheme.typography.bodyMedium,maxLines=3,color=CoddyColors.Muted)
      Provenance(note.source)
     }
    }
   }
  }else{
   item{
    SectionCard{
     Text("A proposal is a draft, not a memory.",style=MaterialTheme.typography.titleLarge)
     Text("Review it here, export it, then approve it in Coddy on Windows.",style=MaterialTheme.typography.bodyMedium,color=CoddyColors.Muted)
     Button(onClick=onProposal,enabled=!ui.storageBlocked,modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text("New proposal")}
     if(reviewed>0)OutlinedButton(onClick=onExport,enabled=ui.busy==null,modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Outlined.UploadFile,null);Spacer(Modifier.width(8.dp));Text("Export reviewed · $reviewed")}
    }
   }
   if(ui.data.proposals.isEmpty())item{EmptyPanel(Icons.Outlined.EditNote,"Nothing waiting for review","New facts, preferences and decisions can start here. Your vault changes only after desktop approval.")}
   items(ui.data.proposals.asReversed(),key={it.id}){proposal->
    SectionCard{
     StatusChip(when(proposal.state){ProposalState.DRAFT->"Draft";ProposalState.REVIEWED->"Reviewed for export";ProposalState.REJECTED->"Rejected"},if(proposal.state==ProposalState.REVIEWED)CoddyColors.Success else CoddyColors.Muted)
     Text(proposal.title,style=MaterialTheme.typography.titleLarge);Text(proposal.content,style=MaterialTheme.typography.bodyMedium)
     Provenance(proposal.source,"Proposed source")
     if(proposal.state==ProposalState.DRAFT)OutlinedButton(onClick={onReview(proposal.id)},modifier=Modifier.heightIn(min=48.dp)){Text("Review proposal")}
    }
   }
  }
 }
}
