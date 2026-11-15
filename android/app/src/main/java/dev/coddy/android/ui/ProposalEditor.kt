package dev.coddy.android.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.coddy.android.*
import dev.coddy.core.CATEGORIES

@Composable fun ProposalEditor(ui:CoddyUiState,vm:AppViewModel,onClose:()->Unit){
 val draft=ui.data.proposalDraft
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.fillMaxWidth(.94f).widthIn(max=560.dp).fillMaxHeight(.92f).imePadding(),shape=RoundedCornerShape(24.dp),color=CoddyColors.Surface){
   Column{
    Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp,top=8.dp),verticalAlignment=Alignment.CenterVertically){Text("New memory proposal",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);IconButton(onClick=onClose){Icon(Icons.Outlined.Close,"Close proposal; keep draft")}}
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     Text("This saves a draft on your phone. Your vault changes only after desktop approval.",style=MaterialTheme.typography.bodyMedium,color=CoddyColors.Muted)
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){CATEGORIES.forEach{category->FilterChip(selected=draft.category==category,onClick={vm.draft(draft.copy(category=category))},label={Text(category)},modifier=Modifier.heightIn(min=48.dp))}}
     OutlinedTextField(value=draft.title,onValueChange={vm.draft(draft.copy(title=it.take(120)))},label={Text("Title")},singleLine=true,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(value=draft.content,onValueChange={vm.draft(draft.copy(content=it.take(12000)))},label={Text("Proposed memory")},minLines=4,maxLines=8,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(value=draft.source,onValueChange={vm.draft(draft.copy(source=it.take(240)))},label={Text("Source")},modifier=Modifier.fillMaxWidth())
     Text("Importance · ${draft.importance}/5",style=MaterialTheme.typography.bodyMedium)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){(1..5).forEach{value->FilterChip(selected=draft.importance==value,onClick={vm.draft(draft.copy(importance=value))},label={Text(value.toString())},modifier=Modifier.heightIn(min=48.dp))}}
     Text("Confidence · ${(draft.confidence*100).toInt()}%",style=MaterialTheme.typography.bodyMedium)
     Slider(value=draft.confidence.toFloat(),onValueChange={vm.draft(draft.copy(confidence=it.toDouble()))},valueRange=0f..1f,steps=19)
     if(ui.error!=null)Text(ui.error!!,color=CoddyColors.Danger,style=MaterialTheme.typography.bodyMedium)
    }
    Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     OutlinedButton(onClick=onClose,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Keep draft")}
     Button(onClick={if(vm.saveProposal())onClose()},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Save proposal")}
    }
   }
  }
 }
}
