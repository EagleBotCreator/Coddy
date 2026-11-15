package dev.coddy.android.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.coddy.android.*
import dev.coddy.core.*

@Composable fun ConnectScreen(ui:CoddyUiState,vm:AppViewModel,onModel:()->Unit){
 var reveal by remember{mutableStateOf(false)}
 val connection=ui.connectionForm
 val enabled=ui.busy==null&&!ui.generating&&!ui.storageBlocked
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
  Text("Choose where\nCoddy thinks.",style=MaterialTheme.typography.headlineLarge)
  Text("Keep it on this phone, or use a model running on your computer.",style=MaterialTheme.typography.bodyLarge,color=CoddyColors.Muted)
  SectionCard{
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Outlined.Smartphone,null);Text("On this phone",style=MaterialTheme.typography.titleLarge)}
   StatusChip(if(ui.modelReady)"Model loaded" else "No model loaded",if(ui.modelReady)CoddyColors.Success else CoddyColors.Muted)
   Text(if(ui.data.modelLabel.isNotEmpty())ui.data.modelLabel else "Import an instruction-tuned GGUF. Start with a small 0.5–1.5B model for speed.",style=MaterialTheme.typography.bodyMedium)
   Text("Works without Wi-Fi after import. Model files stay in this app’s private storage.",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)
   OutlinedButton(onClick=onModel,enabled=enabled,modifier=Modifier.heightIn(min=48.dp)){Icon(Icons.Outlined.FileOpen,null);Spacer(Modifier.width(8.dp));Text(if(ui.modelReady)"Choose another model" else "Choose GGUF model")}
   if(connection.provider!=ChatProvider.LOCAL)Button(onClick={vm.setProvider(ChatProvider.LOCAL)},enabled=enabled,modifier=Modifier.heightIn(min=48.dp)){Text("Use this phone")}
   else Text("Selected for chat",style=MaterialTheme.typography.labelSmall,color=CoddyColors.Success)
  }
  SectionCard{
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Outlined.Computer,null);Text("Your computer",style=MaterialTheme.typography.titleLarge)}
   StatusChip(ui.connectionStatus)
   Text("Optional · Connect to AnythingLLM Desktop. The current Coddy Windows CLI is not a network server.",style=MaterialTheme.typography.bodyMedium,color=CoddyColors.Muted)
   OutlinedTextField(value=connection.baseUrl,onValueChange={vm.editConnection(it,connection.workspace,ui.token)},label={Text("Computer address")},placeholder={Text("http://192.168.1.8:3001")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=connection.workspace,onValueChange={vm.editConnection(connection.baseUrl,it,ui.token)},label={Text("Workspace slug")},placeholder={Text("coddy")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
   OutlinedTextField(value=ui.token,onValueChange={vm.editConnection(connection.baseUrl,connection.workspace,it)},label={Text("Access token")},visualTransformation=if(reveal)VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={IconButton(onClick={reveal=!reveal}){Icon(if(reveal)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(reveal)"Hide access token" else "Show access token")}},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
   Text("Desktop mode sends your messages and relevant memory excerpts to this address. The computer must be running and reachable.",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)
   if(ui.connectionDirty)Text("Unsaved changes · Save before using this connection for chat.",style=MaterialTheme.typography.bodySmall,color=CoddyColors.Accent)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(onClick=vm::saveConnection,enabled=enabled,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Save")}
    Button(onClick=vm::testConnection,enabled=enabled,modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("Test connection")}
   }
   if(connection.provider!=ChatProvider.DESKTOP)TextButton(onClick={vm.setProvider(ChatProvider.DESKTOP)},enabled=enabled){Text("Use desktop for chat")}
   else Text("Selected for chat · no automatic fallback",style=MaterialTheme.typography.labelSmall,color=CoddyColors.Secondary)
  }
  Provenance("Only explicitly selected files are imported. Memory proposals never write to your vault from this app.","Your data, under your control")
 }
}
