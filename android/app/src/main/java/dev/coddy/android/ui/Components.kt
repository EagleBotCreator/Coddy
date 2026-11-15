package dev.coddy.android.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun dateLabel(value:String)=runCatching{DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(Instant.parse(value))}.getOrDefault(value)

@Composable fun SectionCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
 Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=CoddyColors.Surface){
  Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
 }
}
@Composable fun StatusChip(text:String,color:Color=CoddyColors.Muted){
 Surface(shape=RoundedCornerShape(20.dp),color=color.copy(alpha=.09f)){
  Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Box(Modifier.size(6.dp).background(color,RoundedCornerShape(3.dp)));Text(text,style=MaterialTheme.typography.labelSmall,color=color)
  }
 }
}
@Composable fun SectionLabel(text:String){Text(text,style=MaterialTheme.typography.labelSmall,color=CoddyColors.Muted)}
@Composable fun EmptyPanel(icon:ImageVector,title:String,description:String,action:String?=null,onAction:()->Unit={}){
 SectionCard{
  Icon(icon,null,Modifier.size(28.dp),tint=CoddyColors.Secondary)
  Text(title,style=MaterialTheme.typography.titleLarge)
  Text(description,style=MaterialTheme.typography.bodyMedium,color=CoddyColors.Muted)
  if(action!=null)OutlinedButton(onClick=onAction,modifier=Modifier.heightIn(min=48.dp)){Text(action)}
 }
}
@Composable fun Provenance(source:String,caption:String="Read-only memory"){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Box(Modifier.width(3.dp).height(38.dp).background(CoddyColors.Accent,RoundedCornerShape(2.dp)))
  Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
   Text(caption,style=MaterialTheme.typography.labelSmall,color=CoddyColors.Accent)
   Text(source,style=MaterialTheme.typography.bodySmall,color=CoddyColors.Muted)
  }
 }
}
