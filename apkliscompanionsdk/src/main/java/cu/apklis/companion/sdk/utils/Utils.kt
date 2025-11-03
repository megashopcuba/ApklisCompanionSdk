/*
 * ApklisCompanion SDK
 * Copyright (c) 2025 VirtualShopRest
 *
 * This library provides easy integration with Apklis and ApklisCompanion
 * payment verification systems for Android applications.
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * GitHub: https://github.com/virtualShopRest/ApklisCompanionSdk
 *
 */

package cu.apklis.companion.sdk.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import com.orhanobut.logger.Logger
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import java.util.regex.Pattern

object Utils {
    private const val LICENSE_SCHEME = "apklis_license_payment_check"
    private const val APP_SCHEME = "apklis_payment_check"
    private const val PUBLIC_PEM_KEY = "publicKeyPem"
    private const val LICENSE_UUID = "licenseUuid"
    private const val APKLIS_APP_ID = "cu.uci.android.apklis"
    private const val APKLIS_COMPANION_APP_ID = "cu.apklis.companion"
    private const val APKLIS_COMPANION_WEB_URL = "https://www.megashopcuba.com/apps/apkliscompanion"



    /**
     * Valida el formato de un nombre de paquete Android/Java
     */
    fun validatePackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return false

        // Patrón para validar nombre de paquete: debe contener al menos un punto
        // y cada segmento debe empezar con letra y contener solo letras, números y guiones bajos
        val packagePattern = "^[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+$"
        return Pattern.matches(packagePattern, packageName)
    }

    /**
     * Valida el formato de un UUID
     */
    fun validateUUID(uuidString: String): Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Valida el formato de una clave pública PEM
     */
    fun validatePublicKeyPEM(pemKey: String): Boolean {
        if (pemKey.isBlank()) return false

        return try {
            // Verificar que tenga el formato básico de PEM
            if (!pemKey.contains("-----BEGIN PUBLIC KEY-----") ||
                !pemKey.contains("-----END PUBLIC KEY-----")
            ) {
                return false
            }

            // Extraer el contenido base64
            val base64Content = pemKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "") // Remover espacios en blanco

            // Intentar decodificar y crear la clave pública
            val keyBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Base64.getDecoder().decode(base64Content)
            } else {
                android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
            }
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePublic(spec)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun openApklisCompanionWeb(context: Context){
        context.startActivity(Intent(Intent.ACTION_VIEW, APKLIS_COMPANION_WEB_URL.toUri()))
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.getPackageInfoCompat(packageName, 0)
            true
        } catch (unused: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun Context.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            packageManager.getPackageInfo(packageName, flags)
        }

    fun openAppLink(context: Context, appPackage: String) {
        val isApklisCompanionInstalled = isAppInstalled(context, APKLIS_COMPANION_APP_ID)
        val isApklisInstalled = isAppInstalled(context, APKLIS_APP_ID)

        if (isApklisCompanionInstalled) {
            openApklisCompanionLink(context, appPackage)
        } else if (isApklisInstalled) {
            openApklisLink(context, appPackage)
        } else {
            try {
                val fullLink = getApklisUrl(appPackage)
                context.startActivity(Intent(Intent.ACTION_VIEW, fullLink.toUri()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun openApklisLink(context: Context, appPackage: String): Boolean {

        val isApklisInstalled = isAppInstalled(context, APKLIS_APP_ID)
        val fullLink = getApklisUrl(appPackage)
        return if (isApklisInstalled && openApkLisLink(
                context,
                fullLink
            )
        )
            true
        else {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, fullLink.toUri()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Logger.e("La app Apklis no está instalada en el dispositivo")

            false
        }

    }

    fun openApklisCompanionLink(context: Context, appPackage: String): Boolean {

        val isApklisCompanionInstalled = isAppInstalled(context, APKLIS_COMPANION_APP_ID)
        val fullLink = getApklisCompanionUrl(appPackage)
        return if (isApklisCompanionInstalled && openApkLisPaymentCheckLink(
                context,
                fullLink
            )
        )
            true
        else {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, fullLink.toUri()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Logger.e("La app ApklisCompanion no está instalada en el dispositivo")

            false
        }

    }

    fun openApklisCompanionLicenseLink(
        context: Context,
        applicationId: String,
        licenseUuid: String,
        publicKeyPem: String
    ): Boolean {

        val isApklisCompanionInstalled = isAppInstalled(context, APKLIS_COMPANION_APP_ID)

        return if (isApklisCompanionInstalled && openApkLisPaymentCheckLink(
                context,
                getApklisCompanionLicenseUrl(applicationId, licenseUuid, publicKeyPem)
            )
        )
            true
        else {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, applicationId.toUri()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Logger.e("La app ApklisCompanion no está instalada en el dispositivo")

            false
        }

    }

    private fun openApkLisPaymentCheckLink(context: Context, link: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        val isAppInstalled = isAppInstalled(context, APKLIS_COMPANION_APP_ID)
        if (isAppInstalled) {
            intent.setPackage(APKLIS_COMPANION_APP_ID)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun openApkLisLink(context: Context, link: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        val isAppInstalled = isAppInstalled(context, APKLIS_APP_ID)
        if (isAppInstalled) {
            intent.setPackage(APKLIS_APP_ID)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {

            false
        }
    }


    private fun getApklisUrl(applicationId: String): String {
        return "https://www.apklis.cu/application/$applicationId"
    }

    private fun getApklisCompanionUrl(applicationId: String): String {
        return "${APP_SCHEME}://$applicationId"
    }

    private fun getApklisCompanionLicenseUrl(
        applicationId: String,
        licenseUuid: String,
        publicKeyPem: String
    ): String {
        return "${LICENSE_SCHEME}://$applicationId/?${PUBLIC_PEM_KEY}=$publicKeyPem&${LICENSE_UUID}=$licenseUuid"
    }

}