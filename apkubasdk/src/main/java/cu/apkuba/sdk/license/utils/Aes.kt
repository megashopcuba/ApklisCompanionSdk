package cu.apkuba.sdk.license.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.text.toByteArray

object Aes {


    // FUNCIÓN MEJORADA: Crear cipher genérico
    private fun createCipher(mode: Int): Cipher {
        val keyBytes = key().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val skeySpec = SecretKeySpec(keyBytes, "AES")
        cipher.init(mode, skeySpec)
        return cipher
    }

    // VERSIÓN OPTIMIZADA (opcional - reemplaza las funciones anteriores)
    fun encrypt(plainText: String): String? {
        return try {
            val cipher = createCipher(Cipher.ENCRYPT_MODE)
            val plainBytes = plainText.toByteArray(Charsets.UTF_8)
            val encryptedBytes = cipher.doFinal(plainBytes)
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun decrypt(encryptedData: String): String? {
        return try {
            val cipher = createCipher(Cipher.DECRYPT_MODE)
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private external fun key(): String

}