package dev.coddy.android.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.coddy.android.*
import dev.coddy.core.*

@Composable fun ChatScreen(ui:CoddyUiState,vm:AppViewModel,onConnect:()->Unit,onMemory:()->Unit,onProposal:()->Unit,onClear:()->Unit){
 val list=rememberLazyListState()
 LaunchedEffect(ui.data.messages.size,ui.partial.length){if(list.layoutInfo.totalItemsCount>0)list.animateScrollToItem(list.layoutInfo.totalItemsCount-1)}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
   Text("Your conversation",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=CoddyColors.Muted)
   if(ui.data.messages.isNotEmpty())TextButton(onClick=onClear,enabled=!ui.generating){Text("Clear chat")}
  }
  LazyColumn(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   if(ui.data.messages.isEmpty())item{
    Column(verticalArrangement=Arrangement.spacedBy(22.dp)){
     Text("What’s on\nyour mind?",style=MaterialTheme.typography.headlineLarge)
     Text("Think something through, make a plan or pick up a thread.",style=MaterialTheme.typography.bodyLarge,color=CoddyColors.Muted)
     if(ui.data.connection.provider==ChatProvider.LOCAL&&!ui.modelReady)EmptyPanel(Icons.Outlined.Smartphone,"Start with a local model","Choose an instruction-tuned GGUF once. Then chat here without the internet.","Choose model",onConnect)
     else SectionCard{StatusChip(if(ui.data.connection.provider==ChatProvider.LOCAL)"Local model ready" else "Desktop mode selected",CoddyColors.Success);Text(if(ui.data.connection.provider==ChatProvider.LOCAL)ui.data.modelLabel else "Replies use the computer you configure in Connect.",style=MaterialTheme.typography.bodyMedium)}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      OutlinedButton(onClick=onMemory,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Search memory")}
      OutlinedButton(onClick=onProposal,modifier=Modifier.weight(1f).heightIn(min=48.dp),enabled=!ui.storageBlocked){Text("Draft a note")}
     }
     Provenance("Imported memories are read-only. New memories go through review.","Memory, with your permission")
    }
   }
   items(ui.data.messages,key={it.id}){message->
    val isUser=message.role=="user"
    Column(Modifier.fillMaxWidth(),horizontalAlignment=if(isUser)Alignment.End else Alignment.Start,verticalArrangement=Arrangement.spacedBy(7.dp)){
     Text(if(isUser)"YOU" else "CODDY",style=MaterialTheme.typography.labelSmall,color=CoddyColors.Muted)
     Surface(shape=RoundedCornerShape(20.dp),color=if(isUser)CoddyColors.Primary else CoddyColors.Surface){
      Text(message.content,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyLarge,color=if(isUser)CoddyColors.Surface else CoddyColors.Primary)
     }
     if(message.stopped)Text("Reply stopped",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Accent)
     if(message.sources.isNotEmpty())Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Context supplied",style=MaterialTheme.typography.labelSmall,color=CoddyColors.Accent);message.sources.forEach{Text(it,style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)}}
    }
   }
   if(ui.generating)item{
    SectionCard{
     Text("CODDY",style=MaterialTheme.typography.labelSmall,color=CoddyColors.Muted)
     if(ui.partial.isBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Text(if(ui.data.connection.provider==ChatProvider.LOCAL)"Thinking on this phone…" else "Waiting for the computer…",style=MaterialTheme.typography.bodyMedium)}
     else Text(ui.partial,style=MaterialTheme.typography.bodyLarge)
    }
   }
  }
  Surface(color=CoddyColors.Surface){
   Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    if(ui.generating)FilledTonalButton(onClick=vm::stop,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Outlined.Stop,null);Spacer(Modifier.width(8.dp));Text("Stop reply")}
    else Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){
     OutlinedTextField(value=ui.data.chatDraft,onValueChange=vm::chatDraft,label={Text("Message Coddy")},supportingText={if(ui.data.chatDraft.length>3500)Text("${ui.data.chatDraft.length}/4000 characters")},isError=ui.data.chatDraft.length>4000,modifier=Modifier.weight(1f),maxLines=5,shape=RoundedCornerShape(18.dp),enabled=!ui.storageBlocked&&ui.busy==null)
     FilledIconButton(onClick=vm::send,modifier=Modifier.size(56.dp),enabled=ui.data.chatDraft.isNotBlank()&&ui.busy==null&&!ui.storageBlocked&&(ui.modelReady||ui.data.connection.provider==ChatProvider.DESKTOP)){Icon(Icons.Outlined.ArrowUpward,"Send message")}
    }
    Text(if(ui.data.connection.provider==ChatProvider.LOCAL)"On this phone · No cloud fallback" else "Sends your message and relevant memory excerpts to your configured computer.",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)
   }
  }
 }
}
