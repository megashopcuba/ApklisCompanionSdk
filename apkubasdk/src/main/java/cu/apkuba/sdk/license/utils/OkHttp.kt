package cu.apkuba.sdk.license.utils

import android.annotation.SuppressLint
import android.os.Build
import cu.apkuba.sdk.BuildConfig
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.tls.HandshakeCertificates
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.text.get

class OkHttp() {
    private var okHttpClient: OkHttpClient? = null
    private val androidNorEarlier: Boolean = Build.VERSION.SDK_INT <= 25

    val httpClient: OkHttpClient
        get() {
            if (okHttpClient == null) {
                buildBaseOkHttpClient()
            }
            return okHttpClient!!
        }


    private fun buildBaseOkHttpClient() {

        val builder = OkHttpClient.Builder()
            .protocols(listOf(Protocol.QUIC, Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor { chain ->

                val builder1: Request.Builder = chain.request().newBuilder()

                    .header(
                        "User-Agent", "Dart/3.5 (dart:io)"
                    )


                val response = chain.proceed(builder1.build())
                response
            }
            .dns(Dns.SYSTEM)
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor(3))
            .connectTimeout(MY_SOCKET_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(MY_SOCKET_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(MY_SOCKET_TIMEOUT_S, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG)
            builder.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
        else
            builder.connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))

        if (androidNorEarlier) {
            // TODO: download fresh from https://letsencrypt.org/certs/isrgrootx1.pem
            val isgCert =
                "-----BEGIN CERTIFICATE-----\n" + "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\n" + "-----END CERTIFICATE-----".trimIndent()

            val cf = CertificateFactory.getInstance("X.509")
            val isgCertificate: Certificate = cf.generateCertificate(
                ByteArrayInputStream(
                    isgCert.toByteArray(
                        charset("UTF-8")
                    )
                )
            )

            val certificates = HandshakeCertificates.Builder()
                .addTrustedCertificate(isgCertificate as X509Certificate) // Uncomment to allow connection to any site generally, but could possibly cause
                // noticeable memory pressure in Android apps.
                //              .addPlatformTrustedCertificates()
                .build()

            builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        }


        okHttpClient = builder.build()
    }

    class RetryInterceptor(private val maxRetries: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request: Request = chain.request()
            var response: Response? = null
            var lastException: IOException? = null

            repeat(maxRetries) { attempt ->
                try {
                    response?.close() // Cerramos la respuesta previa para evitar fugas de recursos
                    response = chain.proceed(request)
                    if (response.isSuccessful) {
                        return response
                    } else if (response.code == 404 || response.code == 403 || response.code == 400 || response.code == 401) {

                        return response
                    }

                } catch (e: IOException) {
                    lastException = e
                    if (attempt == maxRetries - 1) {
                        throw e
                    }
                }
                Thread.sleep(1000) // Espera entre reintentos
            }

            lastException?.let { throw it }
            return response!!
        }
    }

    private var trustAllSslContext: SSLContext? = null

    @SuppressLint("TrustAllX509TrustManager")
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    })

    private lateinit var trustAllSslSocketFactory: SSLSocketFactory

    init {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL")
            trustAllSslContext!!.init(null, trustAllCerts, SecureRandom())
            trustAllSslSocketFactory = trustAllSslContext!!.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()

        }
    }

    fun newCall(request: Request): Call {

        return httpClient.newCall(request)
    }

    fun buildRequestBody(json: String): RequestBody {
        return json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }


    companion object {
        private var singleton: OkHttp? = null
        private const val MY_SOCKET_TIMEOUT_S: Long = 30
        fun with(): OkHttp {
            if (null == singleton) singleton = OkHttp()
            return singleton as OkHttp
        }
    }
}