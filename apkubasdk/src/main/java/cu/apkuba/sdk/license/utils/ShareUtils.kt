package cu.apkuba.sdk.license.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Clase utilitaria para compartir texto
 */
object ShareUtils {

    /**
     * Comparte texto a través de cualquier aplicación que acepte texto (redes sociales, email, etc.)
     *
     * @param context El contexto de la aplicación
     * @param text El texto a compartir
     * @param title El título del diálogo de compartir (opcional)
     */
    fun shareText(context: Context, text: String, title: String = "Compartir mediante") {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }

            // Crea un selector para mostrar todas las opciones disponibles
            val chooserIntent = Intent.createChooser(shareIntent, title)

            // Verifica que exista al menos una app que pueda manejar este intent
            if (shareIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooserIntent)
            } else {
                Toast.makeText(
                    context,
                    "No se encontraron aplicaciones para compartir",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Comparte texto específicamente por correo electrónico
     *
     * @param context El contexto de la aplicación
     * @param text El texto a compartir
     * @param subject El asunto del correo
     * @param email Dirección de correo del destinatario (opcional)
     */
    fun shareViaEmail(context: Context, text: String, subject: String, email: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // Tipo MIME para correo electrónico
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                if (!email.isNullOrEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                }
            }

            val chooserIntent = Intent.createChooser(intent, "Enviar correo mediante")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooserIntent)
            } else {
                Toast.makeText(
                    context,
                    "No se encontraron aplicaciones de correo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al enviar correo: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Comparte texto específicamente a través de WhatsApp
     *
     * @param context El contexto de la aplicación
     * @param text El texto a compartir
     */
    fun shareViaWhatsApp(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.whatsapp" // Especifica WhatsApp como la aplicación destino
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al compartir en WhatsApp: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Comparte texto específicamente a través de Twitter/X
     *
     * @param context El contexto de la aplicación
     * @param text El texto a compartir
     */
    fun shareViaTwitter(context: Context, text: String) {
        try {
            val twitterPackages =
                arrayOf("com.twitter.android", "com.twitter.android.lite", "com.twitter.android.x")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }

            // Intenta encontrar una aplicación de Twitter instalada
            var found = false
            for (packageName in twitterPackages) {
                intent.`package` = packageName
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    found = true
                    break
                }
            }

            if (!found) {
                // Si no se encuentra Twitter, abre el selector general
                val chooserIntent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    "Compartir en Twitter/X"
                )
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al compartir en Twitter: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves the image as PNG to the app's cache directory.
     *
     * @param image Bitmap to save.
     */
    suspend fun shareImage(context: Context, image: Bitmap?) {
        return withContext(Dispatchers.IO) {
            try {
                val imagesFolder =
                    File(context.cacheDir, "images")

                if (!imagesFolder.exists())
                    imagesFolder.mkdirs()
                val file = File(imagesFolder, "shared_image.png")
                val stream = FileOutputStream(file)
                image?.compress(Bitmap.CompressFormat.PNG, 90, stream)
                stream.flush()
                stream.close()
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.abermudez.virtualshop.fileprovider",
                    file
                )
                shareImageUri(context, uri)

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error al compartir",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    /**
     * Shares the PNG image from Uri.
     *
     * @param uri Uri of image to share.
     */
    private fun shareImageUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "image/png"
        context.startActivity(intent)
    }

}