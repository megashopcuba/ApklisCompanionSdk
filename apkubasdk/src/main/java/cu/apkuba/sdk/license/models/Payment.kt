package cu.apkuba.sdk.license.models

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class Payment(
    @SerializedName("qr") val qr: String,

    ) {
    companion object {
        val type: Type = object : TypeToken<Payment>() {}.type
    }
}
// Estado de payment
sealed class PaymentState {
    data object Loading : PaymentState()
    data object ConfirmPayment : PaymentState()
    data class Success(val qr: String) : PaymentState()
    data class Error(val message: String) : PaymentState()
    data object AlreadyPay : PaymentState()
    data object NotPayed : PaymentState()
    data class CheckingError(val message: String) : PaymentState()
}