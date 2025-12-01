package cu.apkuba.sdk.license.connection


import com.orhanobut.logger.Logger
import cu.apkuba.sdk.BuildConfig
import cu.apkuba.sdk.license.utils.OkHttp
import cu.apkuba.sdk.license.utils.StaticAssets.DOMAIN_URL
import cu.apkuba.sdk.license.utils.Utils
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class License() {
    private val okHttp = OkHttp.with()

    suspend fun sendLicensePaymentRequest(licenseUUID: String): Response? {

        try {

            val device = userPreferencesRepository.getApklisAccountDataOnce()?.deviceId
            if (device.isNullOrEmpty()) return null

            val json = """
            {
              "device": "$device"
            }
        """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
            if (BuildConfig.DEBUG) Logger.i("device %s", device)

            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().url(getLicensePaymentUrl(licenseUUID)).post(body)
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()
        }


        return null

    }


    suspend fun sendLicenseVerificationRequest(packageId: String): Response? {

        try {
            val device = userPreferencesRepository.getApklisAccountDataOnce()?.deviceId
            if (device.isNullOrEmpty()) return null
            val json = """
            {
              "package_name": "$packageId",
              "device": "$device"
            }
        """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
//            if (BuildConfig.DEBUG) Logger.i("json %s", json)
            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().url(LICENSE_VERIFICATION_URL).post(body)
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()

        }


        return null

    }

    suspend fun sendCompanionLicenseVerificationRequest(packageId: String): Response? {

        try {
            val device = Utils.getBasicDeviceInfo()
            if (device.isNullOrEmpty()) return null
            val json = """
            {
              "package_name": "$packageId",
              "device": "$device"
            }
        """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
//            if (BuildConfig.DEBUG) Logger.i("json %s", json)
            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().url(LICENSE_VERIFICATION_URL).post(body)
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()

        }


        return null

    }

    suspend fun getAllLicenses(
        status: String? = null, include: Boolean = true
    ): Response? {
        try {
//            val device = userPreferencesRepository.getApklisAccountDataOnce()?.deviceId
//            if (device.isNullOrEmpty()) return null

            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().url(getLicensesUrl(null, status, include))
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()
        }


        return null

    }

    suspend fun cancelLicense(uuid: String): Response? {
        try {
            val device = userPreferencesRepository.getApklisAccountDataOnce()?.deviceId
            if (device.isNullOrEmpty()) return null
            val json = """
            {
              "device": "$device"
            }
        """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().post(body).url(cancelLicenseUrl(uuid))
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()

        }


        return null

    }

    suspend fun migrateLicense(licensesUuids: List<String>): Response? {
        try {
            val device = userPreferencesRepository.getApklisAccountDataOnce()?.deviceId
            if (device.isNullOrEmpty()) return null
            val json = """
            {
              "new_device": "$device",
              "licenses": [
                ${licensesUuids.joinToString(",\n") { "\"$it\"" }}
  ]
            }
        """.trimIndent()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
            val request = userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                Request.Builder().post(body).url(MIGRATE_URL)
                    .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                    .addHeader("Content-Type", "application/json").build()
            }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()

        }


        return null

    }

    companion object {
        private fun getLicensePaymentUrl(licenseUUID: String): String {
            return "$DOMAIN_URL/license/v1/license/$licenseUUID/pay-with-transfermovil/"
        }

        const val LICENSE_VERIFICATION_URL = "$DOMAIN_URL/license/v1/license/verify/"
        const val MIGRATE_URL = "$DOMAIN_URL/license/v1/license/migrate/"
        private fun cancelLicenseUrl(licenseUUID: String): String {
            return "$DOMAIN_URL/license/v1/license/$licenseUUID/cancel/"
        }

        private fun getLicensesUrl(deviceId: String?, status: String?, include: Boolean): HttpUrl {
            val url =
                HttpUrl.Builder().host("api.apklis.cu").scheme("https").addPathSegment("license")
                    .addPathSegment("v1").addPathSegment("license").addPathSegment("history")
            if (deviceId != null) url.addQueryParameter(
                if (include) "device" else "device__exclude",
                deviceId
            )
            if (status != null) url.addQueryParameter("status", status)
            return url.build()
        }
    }

}