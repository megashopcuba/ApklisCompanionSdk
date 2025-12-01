package cu.apkuba.sdk.license

import android.app.Application
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.abermudez.virtualshop.secure.av
import com.abermudez.virtualshop.utils.Utils.convertApklisDate
import com.google.gson.Gson
import cu.apkuba.sdk.core.ApKuba
import cu.apkuba.sdk.license.models.ErrorCode
import cu.apkuba.sdk.license.models.LicenseState
import cu.apkuba.sdk.license.models.PaymentState
import cu.apkuba.sdk.license.models.Task
import cu.apkuba.sdk.license.models.TransferMovil
import cu.apkuba.sdk.license.services.MockApiService
import cu.apkuba.sdk.license.utils.DataGetter
import cu.apkuba.sdk.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope


class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService by lazy { MockApiService.with() }
    private val gson by lazy { Gson() }
    private val apklisIntegrityCheck = MutableLiveData(false)
    private val apklisDataCheck = MutableLiveData(false)
    private val apKubaIntegrityCheck = MutableLiveData(false)
    private val _errors = MutableLiveData<HashMap<String, String>>()
    val errors: MutableLiveData<HashMap<String, String>>
        get() = _errors

    private val _loading = MutableLiveData<LoadingState>()
    val loading: MutableLiveData<LoadingState>
        get() = _loading

    private val _activePage = MutableLiveData(0)
    val activePage: MutableLiveData<Int>
        get() = _activePage

    private val _currentTask = MutableLiveData<Task?>()
    val currentTask: MutableLiveData<Task?>
        get() = _currentTask

    private val _transfermovil = MutableLiveData<TransferMovil?>()
    val transfermovil: MutableLiveData<TransferMovil?>
        get() = _transfermovil
    private val _loadingPayment = MutableLiveData<PaymentState>()
    val loadingPayment: MutableLiveData<PaymentState>
        get() = _loadingPayment
    private val _checkingPayment = MutableLiveData<LicenseState>()
    val checkingPayment: MutableLiveData<LicenseState>
        get() = _checkingPayment


    suspend fun buyPlan() = supervisorScope {
        if (apklisIntegrityCheck.value == false)
            checkApkLisIntegrity()
        else if (apklisDataCheck.value == false)
            getApklisData()
        else
            buySelectedPlan()


    }

    private fun addError(key: String, value: String) {
        val errors = _errors.value ?: hashMapOf()
        errors[key] = value
        _errors.postValue(errors)

    }


    private suspend fun checkApkLisCompanionIntegrity() {
        val task =
            Task(getApplication<Application>().getString(R.string.check_apklis_companion_integrity))
        _currentTask.value = task
        try {
            delay(400)
            if (ApKuba.validateAppSignature(getApplication<Application>())) {
                _currentTask.postValue(task.copy(state = TaskState.Success))
                delay(300)
                checkLicencePaymentByApKuba()
            } else {
                apKubaIntegrityCheck.value = true
                _currentTask.postValue(
                    task.copy(
                        state = TaskState.Error(
                            getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail)
                                .toString(), true
                        )
                    )
                )
                addError(
                    getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail)
                        .toString(),
                    getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail_details)
                        .toString()
                )
                delay(300)


            }
        } catch (_: Exception) {
            _currentTask.postValue(
                task.copy(
                    state = TaskState.Error(
                        getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail)
                            .toString(), true
                    )
                )
            )
            addError(
                getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail)
                    .toString(),
                getApplication<Application>().getText(R.string.check_apklis_companion_integrity_fail_details)
                    .toString()
            )
            delay(300)

        }


    }


    private suspend fun checkLicencePaymentByApKuba() {
        val task =
            Task(getApplication<Application>().getString(R.string.check_payment_in_companion))
        _currentTask.value = task
        delay(400)
        try {
            val apklisCheck = coroutineScope.async {
                ApKuba.isLicensePurchased(
                    getApplication<Application>(),
                    app()
                )
            }.await()


            if (apklisCheck.isValid()) {
                val selectedPlan = selectedPlan.value
                if (selectedPlan?.uuid == apklisCheck.licenseUuid) {

                    _selectedPlan.value = selectedPlan?.copy(
                        active = true,
                        expireIn = apklisCheck.expiredIn?.convertApklisDate()
                    )
                    _currentTask.value = task.copy(
                        name = "Compra realizada con éxito",
                        state = TaskState.Success
                    )
                } else {

                    task.copy(
                        state = TaskState.Error(
                            "La licencia activa no coincide con la que se envió en la solicitud.\nReinicie la aplicación.",
                            true
                        )
                    )
                    addError(
                        "La licencia activa no coincide con la que se envió en la solicitud.",
                        "La licencia activa no coincide con la que se envió en la solicitud." +
                                "\nReinicie la aplicación."
                    )
                }
            } else {
                _currentTask.value = task.copy(
                    state = TaskState.Error(
                        apklisCheck.message?.message ?: "Ocurrio un error al comprobar el pago",
                        true
                    ),
                )
                addError(
                    "Ocurrio un error al comprobar el pago",
                    apklisCheck.message?.message ?: "Ocurrio un error al comprobar el pago"
                )
                delay(300)
                selectedPlan.value?.let {
                    Utils.openApKubaLicenseLink(
                        getApplication<Application>(),
                        app(),
                        it.uuid,
                        publicKeyPem()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _currentTask.value =
                task.copy(state = TaskState.Error(e.message ?: "Ocurrio un error", true))
            addError(
                "Ocurrio un error al comprobar el pago",
                "Ocurrio un error al comprobar el pago: ${e.message}"
            )
        }

    }


    private suspend fun checkApkLisIntegrity() {
        val task = Task(getApplication<Application>().getString(R.string.check_apklis_integrity))
        _currentTask.value = task
        delay(400)
        try {
            if (av().validateApkLisSignature(getApplication<Application>())) {
                apklisIntegrityCheck.value = true
                _currentTask.value = task.copy(state = TaskState.Success)
                delay(300)
                getApklisData()

            } else {
                _currentTask.value = task.copy(
                    state = TaskState.Error(
                        getApplication<Application>().getText(R.string.check_apklis_integrity_fail)
                            .toString(), true
                    )
                )
                addError(
                    getApplication<Application>().getText(R.string.check_apklis_integrity_fail)
                        .toString(),
                    getApplication<Application>().getText(R.string.check_apklis_integrity_fail_details)
                        .toString()
                )
                delay(300)
//                checkApkLisCompanionIntegrity()

            }
        } catch (_: Exception) {
            _currentTask.value = task.copy(
                state = TaskState.Error(
                    getApplication<Application>().getText(R.string.check_apklis_integrity_fail)
                        .toString(), true
                )
            )
            addError(
                getApplication<Application>().getText(R.string.check_apklis_integrity_fail)
                    .toString(),
                getApplication<Application>().getText(R.string.check_apklis_integrity_fail_details)
                    .toString()
            )
            delay(300)
//            checkApkLisCompanionIntegrity()

        }


    }

    private suspend fun getApklisData() {
        val task = Task(getApplication<Application>().getString(R.string.get_apklis_data))
        _currentTask.value = task
        delay(400)
        try {
            val data = DataGetter.getApklisAccountData(getApplication<Application>())
            if (data == null) {
                userPreferencesRepository.logout()
                _currentTask.value = task.copy(
                    state = TaskState.Error(
                        getApplication<Application>().getText(R.string.checking_apklis_data_error)
                            .toString(), true
                    )
                )
                addError(
                    getApplication<Application>().getText(R.string.checking_apklis_data_error)
                        .toString(),
                    getApplication<Application>().getText(R.string.checking_apklis_data_error_details)
                        .toString()
                )
                delay(300)
//                checkApkLisCompanionIntegrity()
                return
            }
            userPreferencesRepository.saveApklisAccountData(data)
            _currentTask.value = task.copy(state = TaskState.Success)
            delay(300)

            _currentTask.value = task.copy(
                name = getApplication<Application>().getString(R.string.checking_apklis_data),
                state = TaskState.Loading
            )
            val userData = viewModelScope.async(Dispatchers.IO) { apiService.getMe() }.await()
            if (userData?.isValidUser() == true) {
                _currentTask.value = task.copy(
                    name = getApplication<Application>().getString(R.string.checking_apklis_data),
                    state = TaskState.Success
                )
                apklisDataCheck.value = true

                delay(300)
                buySelectedPlan()
            } else {
                _currentTask.value = task.copy(
                    name = getApplication<Application>().getString(R.string.checking_apklis_data),
                    state = TaskState.Error(
                        getApplication<Application>().getText(R.string.checking_apklis_data_error)
                            .toString(), true
                    )
                )
                addError(
                    getApplication<Application>().getText(R.string.checking_apklis_data_error)
                        .toString(),
                    getApplication<Application>().getText(R.string.checking_apklis_data_error_details)
                        .toString()
                )
//                delay(300)
//                checkApkLisCompanionIntegrity()

            }


        } catch (e: Exception) {
            e.printStackTrace()
            _currentTask.value = task.copy(
                state = TaskState.Error(
                    getApplication<Application>().getText(R.string.checking_apklis_data_error)
                        .toString(), true
                )
            )
            addError(
                getApplication<Application>().getText(R.string.checking_apklis_data_error)
                    .toString(),
                "${getApplication<Application>().getText(R.string.checking_apklis_data_error_details)}\n${e.message}"
            )
//            delay(300)
//            checkApkLisCompanionIntegrity()

        }


    }

    suspend fun buySelectedPlan() {
        val task =
            Task(getApplication<Application>().getString(R.string.buying_licence_from_apklis))
        _currentTask.value = task
        _loadingPayment.value = PaymentState.Loading
        delay(400)
        try {
            if (selectedPlan.value?.uuid == null) return

            val licenseState =
                viewModelScope.async(Dispatchers.IO) {
                    selectedPlan.value?.uuid?.let {
                        apiService.sendLicencePaymentRequest(
                            it
                        )
                    }
                }.await()
            _loadingPayment.value = licenseState!!

            when (licenseState) {
                is PaymentState.Success -> {
                    prepareTransfermovil(licenseState.qr)
                    _currentTask.value = task.copy(state = TaskState.Success)
                    delay(300)
                }

                is PaymentState.Error -> {
                    _currentTask.value = task.copy(
                        state = TaskState.Error(
                            licenseState.message, true
                        )
                    )
                    addError(
                        getApplication<Application>().getText(R.string.buying_licence_from_apklis_error)
                            .toString(),
                        licenseState.message
                    )
                }

                PaymentState.AlreadyPay -> {
                    _currentTask.value = task.copy(
                        state = TaskState.Error(
                            "Ya has pagado una licencia.\nReinicie la aplicación.", true
                        )
                    )
                    addError(
                        "Ya has pagado una licencia.",
                        "Ya has pagado una licencia.\nReinicie la aplicación."
                    )

                }

                PaymentState.NotPayed -> {
                    _currentTask.value = task.copy(
                        state = TaskState.Error(
                            "El pago de la licencia está pendiente.\n\nSi perdió el QR de pago debe ir al apartado de licencias en Apklis o Apklis companion y cancelar la que está pendiente y luego volver y probar de nuevo.\n\n" +
                                    "Si ya pagó la licencia espere un rato a que se refleje la compra en la plataforma, debe reiniciar la aplicación para comprobar el estado de la licencia.",
                            true
                        )
                    )
                    addError(
                        "El pago de la licencia está pendiente.",
                        "Si perdió el QR de pago debe ir al apartado de licencias en Apklis o Apklis companion y cancelar la que está pendiente y luego volver y probar de nuevo.\n\n" +
                                "Si ya pagó la licencia espere un rato a que se refleje la compra en la plataforma, debe reiniciar la aplicación para comprobar el estado de la licencia."
                    )
                }

                else -> {
                    _currentTask.value = task.copy(
                        state = TaskState.Error(
                            "Ocurrió un error", true
                        )
                    )
                    addError(
                        getApplication<Application>().getText(R.string.buying_licence_from_apklis_error)
                            .toString(),
                        getApplication<Application>().getText(R.string.buying_licence_from_apklis_error_details)
                            .toString(),
                    )

                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
            _currentTask.value = task.copy(
                state = TaskState.Error(
                    e.message.toString(), true
                )
            )
            addError(
                getApplication<Application>().getText(R.string.buying_licence_from_apklis_error)
                    .toString(),
                "${getApplication<Application>().getText(R.string.buying_licence_from_apklis_error_details)}\n${e.message}",
            )
        }


    }

    suspend fun sendLicenceVerificationRequest() {
        val task = Task(getApplication<Application>().getString(R.string.check_licence_from_apklis))
        _currentTask.value = task
        _checkingPayment.value = LicenseState.Loading
        val license = viewModelScope.async(Dispatchers.IO) {
            apiService.sendLicenceVerificationRequest(getApplication<Application>())
        }.await()

        _currentTask.value = when (license) {

            is LicenseState.Error -> {
                task.copy(
                    state = TaskState.Error(
                        license.message, true
                    )
                )
            }

            is LicenseState.ConfirmLicense -> {
                val selectedPlan = selectedPlan.value
                if (selectedPlan?.uuid == license.licenseResponse.license) {
                    _checkingPayment.value =
                        LicenseState.Success
                    _selectedPlan.value = selectedPlan.copy(active = true)
                    task.copy(
                        name = "Compra realizada con éxito",
                        state = TaskState.Success
                    )
                } else {
                    addError(
                        "La licencia activa no coincide",
                        "La licencia activa no coincide con la que se envió en la solicitud.\nReinicie la aplicación.",
                    )
                    _checkingPayment.value =
                        LicenseState.Error(
                            "La licencia activa no coincide",
                            ErrorCode.LICENSE_NOT_MATCH
                        )
                    task.copy(
                        state = TaskState.Error(
                            "La licencia activa no coincide con la que se envió en la solicitud.\nReinicie la aplicación.",
                            true
                        )
                    )
                }
            }

            LicenseState.NotPayed -> {
                addError(
                    "El pago de la licencia está pendiente",
                    "El pago de la licencia está pendiente aún, si ya pagó espere un rato a que se refleje la compra en la plataforma, debe reiniciar la aplicación para comprobar el estado de la licencia.",
                )
                _checkingPayment.value =
                    LicenseState.Error("El pago de la licencia está pendiente", ErrorCode.NOT_PAYED)
                task.copy(
                    state = TaskState.Error(
                        "El pago de la licencia está pendiente aún.", true
                    )
                )

            }

            else -> {
                addError(
                    getApplication<Application>().getText(R.string.check_licence_from_apklis_error)
                        .toString(),
                    "${getApplication<Application>().getText(R.string.check_licence_from_apklis_error_details)}",
                )
                _checkingPayment.value =
                    LicenseState.Error("Ocurrió un error", ErrorCode.UNKNOWN_ERROR)
                task.copy(
                    state = TaskState.Error(
                        "Ocurrió un error.", true
                    )
                )

            }

        }


    }

    private fun prepareTransfermovil(qr: String) {
        _transfermovil.value = gson.fromJson(qr, TransferMovil.type)

    }


    suspend fun checkLicense(callBack: (plan: String?, error: String?) -> Unit) {


        val licence =
            viewModelScope.async(Dispatchers.IO) {
                apiService.sendLicenceVerificationRequest(
                    getApplication<Application>()
                )
            }
                .await()

        when (licence) {
            is LicenseState.ConfirmLicense -> {
                val plan = plans.values.find { it.uuid == licence.licenseResponse.license }
                if (plan != null) {
                    val updatedPlan = plan.copy(
                        active = true,
                        expireIn = licence.licenseResponse.expireIn
                    )
                    userPreferencesRepository.saveActivePlan(updatedPlan)
                    _selectedPlan.postValue(updatedPlan)
                    callBack(licence.licenseResponse.license, null)
                } else {
                    addError(
                        "La licencia activa no coincide con la que se envió en la solicitud.",
                        "Descargue la aplicación de los sitios oficiales, puede estar desactualizada o modificada la aplicación.",
                    )
                    callBack(
                        null,
                        "La licencia activa no coincide con la que se envió en la solicitud."
                    )
                }
            }

//            is LicenseState.Error -> {
//                callBack(null, licence.message)
//            }
//
//            LicenseState.NotPayed -> {
//                callBack(null, "Debe pagar un plan de licencia.")
//            }


            else -> {

                val apklisCheck = viewModelScope.async {
                    ApKuba.isLicensePurchased(
                        getApplication<Application>(),
                        app()
                    )
                }.await()

                if (apklisCheck.isValid()) {
                    val plan = plans.values.find { it.uuid == apklisCheck.licenseUuid }
                    if (plan != null) {
                        val updatedPlan = plan.copy(
                            active = true,
                            expireIn = apklisCheck.expiredIn
                        )
                        userPreferencesRepository.saveActivePlan(updatedPlan)
                        _selectedPlan.postValue(updatedPlan)

                        callBack(apklisCheck.licenseUuid, null)

                    } else {
                        addError(
                            "La licencia activa no coincide con la que se envió en la solicitud.",
                            "Descargue la aplicación de los sitios oficiales, puede estar desactualizada o modificada la aplicación.",
                        )
                        callBack(
                            null,
                            "La licencia activa no coincide con la que se envió en la solicitud."
                        )
                    }
                } else {
                    val licence =
                        viewModelScope.async(Dispatchers.IO) {
                            apiService.sendCompanionLicenceVerificationRequest(
                                getApplication<Application>()
                            )
                        }
                            .await()
                    when (licence) {
                        is LicenseState.ConfirmLicense -> {
                            val plan =
                                plans.values.find { it.uuid == licence.licenseResponse.license }
                            if (plan != null) {
                                val updatedPlan = plan.copy(
                                    active = true,
                                    expireIn = licence.licenseResponse.expireIn
                                )
                                userPreferencesRepository.saveActivePlan(updatedPlan)
                                _selectedPlan.postValue(updatedPlan)
                                callBack(licence.licenseResponse.license, null)
                            } else {
                                addError(
                                    "La licencia activa no coincide con la que se envió en la solicitud.",
                                    "Descargue la aplicación de los sitios oficiales, puede estar desactualizada o modificada la aplicación.",
                                )
                                callBack(
                                    null,
                                    "La licencia activa no coincide con la que se envió en la solicitud."
                                )
                            }
                        }

                        is LicenseState.Error -> {
                            when (licence.errorCode) {
                                ErrorCode.UNAUTHORISED -> addError(
                                    licence.message,
                                    "Abra la app de Apklis y acceda con su cuenta y vuelva a intentarlo.",
                                )

                                ErrorCode.UNKNOWN_ERROR -> TODO()
                                ErrorCode.NOT_FOUND -> addError(
                                    licence.message,
                                    "Si compro su licencia en Apklis companion, abra la app de Apklis y migre la licencia, luego vuelva a intentarlo.",
                                )

                                ErrorCode.NOT_PAYED -> addError(
                                    licence.message,
                                    "El pago de la licencia está pendiente aún, si ya pagó espere un rato a que se refleje la compra en la plataforma, debe reiniciar la aplicación para comprobar el estado de la licencia.",
                                )

                                ErrorCode.BAD_REQUEST ->
                                    addError(
                                        licence.message,
                                        "Cheque lo siguiente: <br>1-Compruebe su conexón <br>2-Tener la última versión de la app de Apklis <br>3-Esta logueado correctamente en la app de Apklis <br>4-Cerre la sesión en la app de Apklis y volvelva a acceder e intente de nuevo.",
                                    )

                                ErrorCode.LICENSE_NOT_FOUND -> addError(
                                    licence.message,
                                    "El plan no se encontró en la plataforma.",
                                )

                                ErrorCode.LICENSE_ALREADY_ACTIVATED -> {}
                                ErrorCode.LICENSE_ALREADY_CANCELLED -> addError(
                                    licence.message,
                                    "El plan fue cancelado, debe adquirir un nuevo plan de licencia.",
                                )

                                ErrorCode.LICENSE_ALREADY_EXPIRED -> addError(
                                    licence.message,
                                    "El plan venció, debe adquirir un nuevo plan de licencia.",
                                )

                                ErrorCode.TOO_MANY_REQUESTS -> {}
                                ErrorCode.MAX_TRAY_REACHED -> addError(
                                    "Se alcanzó el número de intentos de verificación",
                                    "Cheque lo siguiente: <br>1-Compruebe su conexón <br>2-Tener la última versión de la app de Apklis <br>3-Esta logueado correctamente en la app de Apklis <br>4-Cerre la sesión en la app de Apklis y volvelva a acceder e intente de nuevo.",

                                    )

                                ErrorCode.SIGNATURE_INVALID -> addError(
                                    licence.message,
                                    "Cheque lo siguiente: <br>1-Compruebe su conexón <br>2-Tener la última versión de la app de fuentes oficiales ",

                                    )

                                else -> {}

                            }

                            callBack(null, licence.message)
                        }

                        LicenseState.NotPayed -> {
                            callBack(null, "Debe pagar un plan de licencia.")
                        }

                        else -> {
                            callBack(null, apklisCheck.message?.message)
                        }

                    }

                }

            }


        }
    }


    @Keep
    private external fun app(): String

    @Keep
    private external fun publicKeyPem(): String


}







