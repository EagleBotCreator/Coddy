package dev.coddy.android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import dev.coddy.android.ui.CoddyApp
import dev.coddy.android.ui.CoddyTheme
class MainActivity:ComponentActivity(){
 private val vm by lazy{ViewModelProvider(this)[AppViewModel::class.java]}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();setContent{CoddyTheme{CoddyApp(vm)}}}
 override fun onStop(){vm.flush();super.onStop()}
}
