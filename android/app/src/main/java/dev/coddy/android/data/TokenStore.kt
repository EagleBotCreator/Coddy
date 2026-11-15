package dev.coddy.android.data
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Ciphertext lives atomically beside its address; AAD also binds it to that destination. */
class TokenStore(@Suppress("UNUSED_PARAMETER") context:Context){
 private val alias="coddy-desktop-token"
 private fun key():SecretKey{
  val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (store.getKey(alias,null) as? SecretKey)?.let{return it}
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
   init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  }.generateKey()
 }
 fun read(destination:String,payload:String):String{
  if(payload.isBlank())return ""
  val combined=Base64.decode(payload,Base64.NO_WRAP)
  require(combined.size>12){"Saved token is unreadable."}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{
   init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,combined.copyOfRange(0,12)))
   updateAAD(destination.toByteArray(Charsets.UTF_8))
  }
  return cipher.doFinal(combined.copyOfRange(12,combined.size)).toString(Charsets.UTF_8)
 }
 fun seal(destination:String,token:String):String{
  require(destination.isNotBlank()&&token.isNotBlank()){"Enter a valid connection and token."}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,key());updateAAD(destination.toByteArray(Charsets.UTF_8))}
  return Base64.encodeToString(cipher.iv+cipher.doFinal(token.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
 }
}
