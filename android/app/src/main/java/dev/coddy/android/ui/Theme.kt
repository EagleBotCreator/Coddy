package dev.coddy.android.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable fun CoddyTheme(content:@Composable ()->Unit){
 val palette=lightColorScheme(
  primary=CoddyColors.Primary,onPrimary=CoddyColors.Surface,secondary=CoddyColors.Secondary,
  tertiary=CoddyColors.Accent,background=CoddyColors.Background,onBackground=CoddyColors.Primary,
  surface=CoddyColors.Surface,onSurface=CoddyColors.Primary,surfaceVariant=CoddyColors.Background,
  onSurfaceVariant=CoddyColors.Muted,outline=CoddyColors.Border,error=CoddyColors.Danger
 )
 val type=Typography(
  headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=32.sp,lineHeight=38.sp),
  headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=26.sp,lineHeight=32.sp),
  titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=21.sp,lineHeight=27.sp),
  bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),
  bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
  labelSmall=TextStyle(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=16.sp)
 )
 MaterialTheme(colorScheme=palette,typography=type,content=content)
}
