package cu.apkuba.sdk.license.connection

import com.orhanobut.logger.Logger
import cu.apkuba.sdk.BuildConfig
import cu.apkuba.sdk.license.utils.OkHttp
import cu.apkuba.sdk.license.utils.StaticAssets.DOMAIN_URL
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.Response


class Payment() {
    private val okHttp = OkHttp.with()

    suspend fun sendPaymentRequest(app: String): Response? {

        try {

//            val json = """
//            {
//              "package_name": "$app"
//            }
//        """.trimIndent()
//            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
//            val body = json.toRequestBody(mediaType)
            if (BuildConfig.DEBUG) Logger.i("package_name %s", app)
            val formBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("package_name", app)
                .build()
            val request =
                userPreferencesRepository.getApklisAccountDataOnce()?.let { accountData ->
                    Request.Builder()
                        .addHeader("Authorization", "Bearer ${accountData.accessToken}")
                        .url(PAYMENT_URL)
                        .post(formBodyBuilder)
                        .build()
                }

            return request?.let { okHttp.newCall(it).execute() }

        } catch (e: Exception) {
            e.printStackTrace()

        }


        return null

    }


    companion object {
        const val PAYMENT_URL = "${DOMAIN_URL}/v3/application/pay_with_transfermovil/"

    }

}