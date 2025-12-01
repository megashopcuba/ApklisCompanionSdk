package cu.apkuba.sdk.license.services

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import cu.apkuba.sdk.BuildConfig
import cu.apkuba.sdk.license.connection.License
import cu.apkuba.sdk.license.connection.Payment
import cu.apkuba.sdk.license.models.ApklisUser
import cu.apkuba.sdk.license.models.Error
import cu.apkuba.sdk.license.models.ErrorCode
import cu.apkuba.sdk.license.models.LicenseFullData
import cu.apkuba.sdk.license.models.LicenseResponse
import cu.apkuba.sdk.license.models.LicenseState
import cu.apkuba.sdk.license.models.LicenseX
import cu.apkuba.sdk.license.models.PaymentState
import cu.apkuba.sdk.license.models.TransferMovil
import cu.apkuba.sdk.license.utils.Aes
import cu.apkuba.sdk.license.utils.DataGetter
import cu.apkuba.sdk.license.utils.JsonUtils
import cu.apkuba.sdk.license.utils.Utils

interface ApiService {


    suspend fun getFullMe()
    suspend fun getMe(): ApklisUser?

    suspend fun sendPaymentRequest(packageName: String, callCount: Int = 0): PaymentState
    suspend fun sendLicencePaymentRequest(
        licenseUUID: String,
        callCount: Int = 0
    ): PaymentState

    suspend fun sendLicenceVerificationRequest(
        context: Context, callCount: Int = 0,
        retry: Boolean = false
    ): LicenseState

    suspend fun sendCompanionLicenceVerificationRequest(
        context: Context, callCount: Int = 0,
        retry: Boolean = false
    ): LicenseState

    suspend fun loadLicenses(): LicenseState
    suspend fun loadOtherDevicesLicenses(): LicenseState
    suspend fun cancelLicense(uuid: String): LicenseState
    suspend fun migrateLicense(licenses: List<LicenseX>): LicenseState
}


class MockApiService : ApiService {
    private val payment by lazy { Payment() }
    private val license by lazy { License() }
    private val gson by lazy { Gson() }


    override suspend fun getMe(): ApklisUser? {

        val response = user.me()
        val body = response?.body?.string()
        if (response?.isSuccessful == true) {
            val decryptedData = body?.let { Aes.decrypt(it) }
            val userData = JsonUtils.parseString<ApklisUser>(decryptedData, ApklisUser.type)
            userPreferencesRepository.saveApklisUser(userData)
            getFullMe()

            return userData

        }
        response?.close()


        return null
    }

    override suspend fun getFullMe() {
        val response = user.fullMe()
        val body = response?.body?.string()
        if (response?.isSuccessful == true) {
            val userData = JsonUtils.parseString<ApklisUser>(body, ApklisUser.type)
            userPreferencesRepository.saveApklisFullUser(userData)

        }

        response?.close()

    }


    override suspend fun sendPaymentRequest(packageName: String, callCount: Int): PaymentState {
        if (callCount > 3) {
            return PaymentState.Error("Demasiados intentos fallidos")
        }


        val response = payment.sendPaymentRequest(packageName)
        val body = response?.body?.string()

        if (response?.isSuccessful == true) {

            val payment =
                JsonUtils.parseString<cu.apkuba.sdk.license.models.Payment>(
                    body,
                    cu.apkuba.sdk.license.models.Payment.type
                )


            return PaymentState.Success(payment.qr)
        } else if (response?.code == 400) {
            if (body != null) {
                val error =
                    JsonUtils.parseString<Error>(
                        body,
                        Error.type
                    )
                return PaymentState.Error(error.detail ?: "No se pudo generar el pago")
            }

        } else if (response?.code == 401 || response?.code == 403) {
            return PaymentState.Error("No está autorizado")

        }
        response?.close()
        return PaymentState.Error("No se pudo generar el pago")

    }

    override suspend fun sendLicencePaymentRequest(
        licenseUUID: String,
        callCount: Int
    ): PaymentState {
        if (callCount > 3) {
            return PaymentState.Error("Demasiados intentos fallidos")
        }
        val response = license.sendLicensePaymentRequest(licenseUUID)
        val body = response?.body?.string()

        var state: PaymentState = PaymentState.Error("No se pudo generar el pago")

        if (response?.isSuccessful == true) {
            val payment = JsonUtils.parseString<TransferMovil>(
                body,
                TransferMovil.type
            )

            val signatureBase64 = response.headers[SIGNATURE_HEADER_NAME]

            if (signatureBase64.isNullOrEmpty()) {
//                license = license.copy(status = LicenseStatus.SIGNATURE_INVALID)
                state = PaymentState.Error("Invalid response signature")
            } else {
                // Verify signature only on successful response
                val isSignatureValid = Utils.verifySignature(
                    publicKeyPem(),
                    payment.jsonString,
                    signatureBase64
                )

                state = if (!isSignatureValid.first) {
                    //                    license = license.copy(status = LicenseStatus.SIGNATURE_INVALID, qrCode = payment.jsonString, price = payment.price)
                    PaymentState.Error(isSignatureValid.second ?: "Invalid signature")
                } else {
                    //                    license = license.copy(status = LicenseStatus.NOT_PAYED, qrCode = payment.jsonString)
                    PaymentState.Success(payment.jsonString)
                }


            }


        } else if (response?.code == 400) {
            if (body != null) {
                val error = JsonUtils.parseString<Error>(
                    body,
                    Error.type
                )
                state = PaymentState.Error(error.detail ?: "No se pudo generar el pago")
            }

        } else if (response?.code == 401) {
            state = PaymentState.Error("No está autorizado")

        } else if (response?.code == 404) {
            state = PaymentState.Error("No se encontró la licencia")

        } else if (response?.code == 403) {
            if (body != null) {
                val error = JsonUtils.parseString<Error>(body, Error.type)
                if (BuildConfig.DEBUG) Logger.d(error)
                state = when (error.code) {
                    "already_paid" -> PaymentState.AlreadyPay
                    "pending_payment" -> PaymentState.NotPayed
                    else -> {
                        if (error.detail != null) {
                            PaymentState.Error(
                                when (error.detail) {
                                    "There was a problem with the payment service, please try again later." -> "Ha habido un problema con el servicio de pago, por favor, inténtelo de nuevo más tarde"
                                    else -> error.detail
                                }
                            )
                        } else PaymentState.Error("Ocurrió un error")

                    }
                }
            }

        }
        response?.close()
//        loadLicenses()
//        if (state is LicenseState.Success) {
//
//        }

        return state
    }

    override suspend fun sendLicenceVerificationRequest(
        context: Context,
        callCount: Int,
        retry: Boolean
    ): LicenseState {
        if (callCount > 3) {
            return LicenseState.Error(
                "Demasiados intentos fallidos",
                ErrorCode.TOO_MANY_REQUESTS
            )
        }
        val response = license.sendLicenseVerificationRequest(app())
        val body = response?.body?.string()
        var state: LicenseState = LicenseState.Error(
            "Fallo al efectuar la verificación, abra Apklis y autentifiquese.",
            ErrorCode.UNAUTHORISED
        )

        if (response?.isSuccessful == true) {
            val licenseResponse = JsonUtils.parseString<LicenseResponse>(
                body,
                LicenseResponse.type
            )

            val signatureBase64 = response.headers[SIGNATURE_HEADER_NAME]

            if (signatureBase64.isNullOrEmpty()) {
//                license = license.copy(status = LicenseStatus.SIGNATURE_INVALID)
                state = LicenseState.Error(
                    "Invalid response signature",
                    ErrorCode.SIGNATURE_INVALID
                )
            } else {
                // Verify signature only on successful response
                val isSignatureValid = Utils.verifySignature(
                    publicKeyPem(),
                    licenseResponse.jsonString,
                    signatureBase64
                )

                state = if (!isSignatureValid.first) {
                    //                    license = license.copy(status = LicenseStatus.SIGNATURE_INVALID)
                    LicenseState.Error(
                        isSignatureValid.second ?: "Invalid signature",
                        ErrorCode.SIGNATURE_INVALID
                    )
                } else {
                    //                    license = license.copy(status = LicenseStatus.PAYED)

                    LicenseState.ConfirmLicense(licenseResponse)
                }

            }


        } else if (response?.code == 400) {
            if (body != null) {
                val error = JsonUtils.parseString<Error>(
                    body,
                    Error.type
                )
                state = LicenseState.Error(
                    error.detail ?: "Fallo al efectuar la verificación",
                    ErrorCode.BAD_REQUEST
                )
            }

        } else if (response?.code == 402) {
//            license = license.copy(status = LicenseStatus.NOT_PAYED)

            state = LicenseState.NotPayed
        } else if (response?.code == 401) {
            val data = DataGetter.getApklisAccountData(context)
            if (data == null) {
                userPreferencesRepository.logout()
                state = LicenseState.Error(
                    "No está autorizado, abra Apklis y autentifiquese",
                    ErrorCode.UNAUTHORISED
                )
            } else {
                userPreferencesRepository.saveApklisAccountData(data)
                if (retry)
                    state = LicenseState.Error(
                        "No está autorizado, abra Apklis y autentifiquese",
                        ErrorCode.UNAUTHORISED
                    )
                else
                    return sendLicenceVerificationRequest(context, retry = true)
            }


        } else if (response?.code == 404) {
            state = LicenseState.Error(
                "No se encontró la licencia",
                ErrorCode.NOT_FOUND
            )

        } else if (response?.code == 403) {
            state = LicenseState.Error("Debe pagar un plan de licencia.", ErrorCode.NOT_PAYED)

        }
        response?.close()
        return state
    }

    override suspend fun sendCompanionLicenceVerificationRequest(
        context: Context,
        callCount: Int,
        retry: Boolean
    ): LicenseState {
        if (callCount > 3) {
            return LicenseState.Error("Demasiados intentos fallidos", ErrorCode.TOO_MANY_REQUESTS)
        }
        val response = license.sendCompanionLicenseVerificationRequest(app())

        val body = response?.body?.string()
        var state: LicenseState = LicenseState.Error(
            "Fallo al efectuar la verificación, abra Apklis y autentifiquese.",
            ErrorCode.UNAUTHORISED
        )
        if (response?.isSuccessful == true) {
            val licenseResponse = JsonUtils.parseString<LicenseResponse>(
                body,
                LicenseResponse.type
            )

            val signatureBase64 = response.headers[SIGNATURE_HEADER_NAME]

            if (signatureBase64.isNullOrEmpty()) {
//                license = license.copy(status = LicenseStatus.SIGNATURE_INVALID)
                state =
                    LicenseState.Error("Invalid response signature", ErrorCode.SIGNATURE_INVALID)
            } else {
                // Verify signature only on successful response
                val isSignatureValid = Utils.verifySignature(
                    publicKeyPem(),
                    licenseResponse.jsonString,
                    signatureBase64
                )

                state = if (!isSignatureValid.first) {
                    //                    license = license.copy(status = LicenseStatus.SIGNATURE_INVALID)
                    LicenseState.Error(
                        isSignatureValid.second ?: "Invalid signature",
                        ErrorCode.SIGNATURE_INVALID
                    )
                } else {
                    //                    license = license.copy(status = LicenseStatus.PAYED)

                    LicenseState.ConfirmLicense(licenseResponse)
                }

            }


        } else if (response?.code == 400) {
            if (body != null) {
                val error = JsonUtils.parseString<Error>(
                    body,
                    Error.type
                )
                state = LicenseState.Error(
                    error.detail ?: "Fallo al efectuar la verificación",
                    ErrorCode.BAD_REQUEST
                )
            }

        } else if (response?.code == 402) {
//            license = license.copy(status = LicenseStatus.NOT_PAYED)

            state = LicenseState.NotPayed
        } else if (response?.code == 401) {
            val data = DataGetter.getApklisAccountData(context)
            if (data == null) {
                userPreferencesRepository.logout()
                state = LicenseState.Error(
                    "No está autorizado, abra Apklis y autentifiquese",
                    ErrorCode.UNAUTHORISED
                )
            } else {
                userPreferencesRepository.saveApklisAccountData(data)
                if (retry)
                    state = LicenseState.Error(
                        "No está autorizado, abra Apklis y autentifiquese",
                        ErrorCode.UNAUTHORISED
                    )
                else
                    return sendLicenceVerificationRequest(context, retry = true)
            }


        } else if (response?.code == 404) {
            state = LicenseState.Error("No se encontró la licencia", ErrorCode.NOT_FOUND)

        } else if (response?.code == 403) {
            state = LicenseState.Error("Debe pagar un plan de licencia.", ErrorCode.NOT_PAYED)
        }
        response?.close()
//        licenseRepository.clearLicences()
//        licenseRepository.insertLicense(license)
        return state
    }

    override suspend fun loadLicenses(): LicenseState {
        val response = license.getAllLicenses()
        val body = response?.body?.string()


        if (response?.isSuccessful == true) {
            val licenseFullData =
                body?.let {
                    JsonUtils.parseString<List<LicenseFullData>>(
                        it,
                        LicenseFullData.listType
                    )
                }
            if (licenseFullData != null) {
                userPreferencesRepository.saveLicensesData(licenseFullData)
                userPreferencesRepository.clearError()
                return LicenseState.Success

            }
        }
        response?.close()
        userPreferencesRepository.addError()
        return if (userPreferencesRepository.getErrorOnce() >= 3) {
            LicenseState.Error(
                "Se alcanzó el número de intentos de verificación",
                ErrorCode.MAX_TRAY_REACHED
            )

        } else
            LicenseState.Success
    }

    override suspend fun loadOtherDevicesLicenses(): LicenseState {
        val response = license.getAllLicenses(status = "active")
        val body = response?.body?.string()

        if (response?.isSuccessful == true) {
            val apiResponse =
                body?.let {
                    JsonUtils.parseString<List<LicenseFullData>>(
                        it,
                        LicenseFullData.listType
                    )
                }
            if (apiResponse != null) {

//                userPreferencesRepository.saveLicenseData(apiResponse)
                return LicenseState.Success

            }
        }
        response?.close()
        return LicenseState.Error(
            response?.code?.codeToException() ?: "Ocurrió un error",
            ErrorCode.UNKNOWN_ERROR
        )
    }

    override suspend fun migrateLicense(licenses: List<LicenseX>): LicenseState {
        val response = license.migrateLicense(licenses.map { it.uuid })
        response?.body?.string()

        if (response?.isSuccessful == true) {

            return LicenseState.Success

        }
        response?.close()
        return LicenseState.Error(
            response?.code?.codeToException() ?: "Ocurrió un error",
            ErrorCode.UNKNOWN_ERROR
        )
    }


    override suspend fun cancelLicense(uuid: String): LicenseState {
        val response = license.cancelLicense(uuid)
        response?.body?.string()
        if (response?.isSuccessful == true) {
            return LicenseState.Success

        }
        response?.close()
        return LicenseState.Error(
            response?.code?.codeToException() ?: "Ocurrió un error",
            ErrorCode.UNKNOWN_ERROR
        )
    }

    @Keep
    private external fun publicKeyPem(): String

    @Keep
    private external fun app(): String


    companion object {
        private var singleton: MockApiService? = null
        private const val SIGNATURE_HEADER_NAME = "signature"

        fun with(): MockApiService {
            if (null == singleton)
                singleton = MockApiService()
            return singleton as MockApiService
        }
    }
}





