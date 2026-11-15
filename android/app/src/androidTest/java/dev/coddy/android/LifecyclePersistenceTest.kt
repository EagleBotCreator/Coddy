package dev.coddy.android
import android.app.Application
import androidx.lifecycle.*
import androidx.test.platform.app.InstrumentationRegistry
import dev.coddy.android.data.AppStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class LifecyclePersistenceTest{
 @Test fun finishingViewModelKeepsFinalDraftButDoesNotPersistRawConnectionSecrets(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val dir=File(context.cacheDir,"lifecycle-"+UUID.randomUUID()).apply{mkdirs()}
  val app=object:Application(){
   init{attachBaseContext(context)}
   override fun getFilesDir()=dir
   override fun getApplicationContext()=this
  }
  val owner=object:ViewModelStoreOwner{override val viewModelStore=ViewModelStore()}
  try{
   val vm=ViewModelProvider(owner,ViewModelProvider.AndroidViewModelFactory(app))[AppViewModel::class.java]
   val deadline=System.currentTimeMillis()+10000
   while(!vm.ui.value.loaded&&System.currentTimeMillis()<deadline)Thread.sleep(20)
   assertTrue(vm.ui.value.loaded)
   vm.chatDraft("Keep the final draft.")
   vm.editConnection("https://person:do-not-store@example.com?token=do-not-store","coddy","temporary-token")
   vm.flush()
   owner.viewModelStore.clear()
   val saved=runBlocking{AppStore(app).load()}
   assertEquals("Keep the final draft.",saved.chatDraft)
   assertEquals("",saved.connection.baseUrl)
   assertFalse(File(dir,"state.json").readText().contains("do-not-store"))
  }finally{owner.viewModelStore.clear();dir.deleteRecursively()}
 }
}
