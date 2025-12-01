/*
 * ApKuba SDK
 * Copyright (c) 2025 VirtualShopRest
 *
 * This library provides easy integration with Apklis and ApKuba
 * payment verification systems for Android applications.
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * GitHub: https://github.com/virtualShopRest/ApKubaSdk
 *
 */

package cu.apkuba.sdk.core

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import com.orhanobut.logger.Logger
import cu.apkuba.sdk.models.ApklisResponse
import cu.apkuba.sdk.models.PaymentStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

object Apklis {
    private const val APKLIS_PROVIDER = "content://cu.uci.android.apklis.payment.provider/app/"
    private const val APKLIS_PROVIDER_NEW = "content://cu.uci.android.apklis.PaymentProvider/app/"
    private const val APKLIS_PAID = "paid"
    private const val APKLIS_USER_NAME = "user_name"
    private const val APKLIS_PACKAGE = "cu.uci.android.apklis"

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun isPurchasedOld(context: Context, applicationId: String): ApklisResponse {
        val providerURI = (APKLIS_PROVIDER + applicationId).toUri()
        val apklisCheck = ApklisResponse()
        try {
            val contentResolver = context.contentResolver.acquireContentProviderClient(providerURI)
            GlobalScope.async {
                val cursor = contentResolver?.query(providerURI, null, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val apklisPay = cursor.getInt(cursor.getColumnIndexOrThrow(APKLIS_PAID))
                        val apklisUserNAme =
                            cursor.getString(cursor.getColumnIndexOrThrow("user_name"))
                        apklisCheck.userName = apklisUserNAme
                        if (apklisPay > 0) {
                            apklisCheck.status = PaymentStatus.PAYED
                        } else {
                            apklisCheck.status = PaymentStatus.NOT_PAYED
                        }
                    } while (cursor.moveToNext())
                } else {
                    apklisCheck.status = PaymentStatus.ERROR
                    apklisCheck.message =
                        Exception("La aplicación de licencia no se encontró en Apklis")
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    contentResolver?.close()
                } else {
                    @Suppress("DEPRECATION") contentResolver?.release()
                }
                cursor?.close()
            }.await()
        } catch (e: Exception) {
            Logger.e(e, "Apklis Payment")
            return ApklisResponse(status = PaymentStatus.EXCEPTION, message = e)
        }
        return apklisCheck
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun isPurchasedNew(context: Context, packageId: String): ApklisResponse {
        val providerURI = "$APKLIS_PROVIDER_NEW$packageId".toUri()
        val apklisCheck = ApklisResponse()
        try {
            val contentResolver = context.contentResolver.acquireContentProviderClient(providerURI)
            GlobalScope.async {
                val cursor = contentResolver?.query(providerURI, null, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val apklisPay = cursor.getInt(cursor.getColumnIndexOrThrow(APKLIS_PAID))
                        val apklisUserNAme =
                            cursor.getString(cursor.getColumnIndexOrThrow(APKLIS_USER_NAME))
                        apklisCheck.userName = apklisUserNAme

                        if (apklisPay > 0) {
                            apklisCheck.status = PaymentStatus.PAYED
                        } else {
                            apklisCheck.status = PaymentStatus.NOT_PAYED
                        }


                    } while (cursor.moveToNext())

                } else {
                    apklisCheck.status = PaymentStatus.ERROR
                    apklisCheck.message =
                        Exception("La aplicación de licencia no se encontró en Apklis")
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    contentResolver?.close()
                } else {
                    @Suppress("DEPRECATION") contentResolver?.release()
                }
                cursor?.close()
            }.await()
        } catch (e: Exception) {
            Logger.e(e, "Apklis Payment")
            return ApklisResponse(status = PaymentStatus.EXCEPTION, message = e)
        }
        return apklisCheck
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun isPurchased(context: Context, applicationId: String): ApklisResponse {
        try {
            val packageInfo = context.packageManager.getPackageInfo(APKLIS_PACKAGE, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }


            return if (versionCode >= 20250415)
                isPurchasedNew(context, applicationId)
            else
                isPurchasedOld(
                    context,
                    applicationId
                )
        } catch (e: Exception) {
            return ApklisResponse(status = PaymentStatus.EXCEPTION, message = e)

        }

    }


}