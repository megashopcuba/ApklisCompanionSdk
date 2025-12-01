package cu.apkuba.sdk.license.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

object Utils {

    fun verifySignature(
        publicKeyPem: String,
        dataString: String,
        signatureBase64: String,
        charset: Charset = Charsets.UTF_8
    ): Pair<Boolean, String?> {
        return verifySignatureWithDetails(
            publicKeyPem, dataString.toByteArray(charset), signatureBase64
        )
    }

    fun getBasicDeviceInfo(encrypted: Boolean = true): String? {
        val info =
            "${Build.BRAND}/${Build.PRODUCT}/${Build.DEVICE}:${getVersionRelease()}/${Build.ID}/${Build.VERSION.INCREMENTAL}:${Build.TYPE}/${Build.TAGS}"
        return (if (encrypted) Aes.encrypt(info) else info)?.trimIndent()?.replace("\n", "")?.trim()
    }

    private fun getVersionRelease(): String {
        val versionRelease = Build.VERSION.RELEASE.toFloat().toInt()

        return versionRelease.toString()
    }

    private fun verifySignatureWithDetails(
        publicKeyPem: String, data: ByteArray, signatureBase64: String
    ): Pair<Boolean, String?> {
        return try {
            val cleanedKey = publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replace("\\s".toRegex(), "")

            if (cleanedKey.isEmpty()) {
                return Pair(false, "Clave pública vacía")
            }

            if (signatureBase64.isEmpty()) {
                return Pair(false, "Firma vacía")
            }

            if (data.isEmpty()) {
                return Pair(false, "Datos vacíos")
            }

            val publicKeyBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getDecoder().decode(cleanedKey)
            } else {
                android.util.Base64.decode(cleanedKey, android.util.Base64.DEFAULT)
            }

            val keySpec = X509EncodedKeySpec(publicKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getDecoder().decode(signatureBase64)
            } else {
                android.util.Base64.decode(signatureBase64, android.util.Base64.DEFAULT)
            }

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(data)

            val isValid = signature.verify(signatureBytes)
            Pair(isValid, if (isValid) null else "Firma inválida")

        } catch (e: IllegalArgumentException) {
            Pair(false, "Error en formato Base64: ${e.message}")
        } catch (e: Exception) {
            Pair(false, "Error de verificación: ${e.message}")
        }
    }

    fun copy(context: Context, content: String, title: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            context,
            "Datos copiados", Toast.LENGTH_SHORT
        ).show()
    }

    private const val apklisLicenseDateFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    fun isNotApklisLicenseExpired(serverTime: String?, expireAt: String): Boolean {
        var timeInMilliseconds: Long = 0
        try {
            val ISODate = expireAt.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
                .replace(Regex("\\.\\d{6}"), ".000")

            val sdf = SimpleDateFormat(apklisLicenseDateFormatPattern, Locale.getDefault())
            val mDate = sdf.parse(ISODate)!!
            timeInMilliseconds = mDate.time


        } catch (e: Exception) {
            e.printStackTrace()

        }

        return timeInMilliseconds > getTimeInMilliseconds(serverTime)
    }

    private const val dateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss zzz"

    private fun getTimeInMilliseconds(dateTime: String?): Long {
        var timeInMilliseconds: Long = 0
        try {
            timeInMilliseconds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ofPattern(dateFormatPattern, Locale.ENGLISH)
                val localDate = LocalDateTime.parse(dateTime, formatter)
                localDate.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli()
            } else {
                val sdf = SimpleDateFormat(dateFormatPattern, Locale.ENGLISH)
                val mDate = sdf.parse(dateTime!!)!!
                mDate.time
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return timeInMilliseconds
    }


    /**
     * Verifica si el dispositivo es Xiaomi/MIUI
     */
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
                Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.equals("Redmi", ignoreCase = true)
//                ||                isPropertyExists("ro.miui.ui.version.name")
    }

    private fun isPropertyExists(property: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("getprop $property")
            process.inputStream.bufferedReader().readText().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}