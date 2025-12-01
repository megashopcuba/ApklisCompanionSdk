package cu.apkuba.sdk.license.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Helper class para gestionar permisos en Android
 * Con soporte especial para dispositivos Xiaomi/MIUI
 */
class PermissionHelper(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE_APKLIS = 1001
        const val REQUEST_CODE_XIAOMI_PERMISSIONS = 1002
        const val APKLIS_READ_ACCOUNT_PERMISSION = "cu.uci.android.apklis.READ_ACCOUNT_DATA"
    }



    /**
     * Abre la configuración de permisos específica de Xiaomi
     */
    fun openXiaomiPermissionSettings() {
        try {
            // Intento 1: Configuración de permisos de MIUI
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", activity.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivityForResult(intent, REQUEST_CODE_XIAOMI_PERMISSIONS)
        } catch (e: Exception) {
            try {
                // Intento 2: Configuración general de permisos de Android
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${activity.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivityForResult(intent, REQUEST_CODE_XIAOMI_PERMISSIONS)
            } catch (e2: Exception) {
                // Intento 3: Configuración general del sistema
                val intent = Intent(Settings.ACTION_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Verifica si un permiso está otorgado
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita un permiso específico
     */
    fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            requestCode
        )
    }

    /**
     * Solicita múltiples permisos
     */
    fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            requestCode
        )
    }

    /**
     * Verifica y solicita el permiso de APKlis si no está otorgado
     * Con soporte especial para Xiaomi
     */
    fun checkAndRequestApklisPermission(): Boolean {
        return if (!isPermissionGranted(APKLIS_READ_ACCOUNT_PERMISSION)) {
            if (Utils.isXiaomiDevice()) {
                // En Xiaomi, redirigir directamente a configuración
                openXiaomiPermissionSettings()
                false
            } else {
                requestPermission(APKLIS_READ_ACCOUNT_PERMISSION, REQUEST_CODE_APKLIS)
                false
            }
        } else {
            true
        }
    }




    /**
     * Verifica si se debe mostrar una explicación del permiso
     */
    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Verifica si APKlis está instalado en el dispositivo
     */
    fun isApklisInstalled(): Boolean {
        return try {
            activity.packageManager.getPackageInfo("cu.uci.android.apklis", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_APKLIS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}

/**
 * Interfaz para callbacks de permisos
 */
interface PermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied()
    fun onPermissionPermanentlyDenied()
}