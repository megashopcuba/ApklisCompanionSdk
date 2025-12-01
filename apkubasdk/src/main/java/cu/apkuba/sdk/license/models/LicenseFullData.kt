package cu.apkuba.sdk.license.models

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import java.lang.reflect.Type

@Serializable
data class LicenseFullData(
    @SerializedName("activated_at") val activatedAt: String?,
    @SerializedName("cancelled_at") val cancelledAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("device") val device: String,
    @SerializedName("expire_in") val expireIn: String?,
    @SerializedName("expired_at") val expiredAt: String?,
    @SerializedName("license") val license: LicenseX,
    @SerializedName("license_group") val licenseGroup: LicenseGroup,
    @SerializedName("status") val status: String?,
    @SerializedName("status_display") val statusDisplay: String?,
    @SerializedName("uuid") val uuid: String
) {
    companion object {
        val type: Type = object : TypeToken<LicenseFullData>() {}.type
        val listType: Type = object : TypeToken<List<LicenseFullData>>() {}.type

    }
}

@Serializable
data class LicenseX(
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("uuid") val uuid: String
)

@Serializable
data class LicenseGroup(
    @SerializedName("icon") val icon: String?,
    @SerializedName("name") val name: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("uuid") val uuid: String
)

// Estado de payment
sealed class LicenseState {
    data object Loading : LicenseState()
    data class ConfirmLicense(val licenseResponse: LicenseResponse) : LicenseState()
    data object Success : LicenseState()
    data object NotPayed : LicenseState()
    data class Error(val message: String,val errorCode: ErrorCode) : LicenseState()
}

enum class ErrorCode{
    UNAUTHORISED,
    UNKNOWN_ERROR,
    NOT_FOUND,
    NOT_PAYED,
    BAD_REQUEST,
    LICENSE_NOT_FOUND,
    LICENSE_ALREADY_ACTIVATED,
    LICENSE_ALREADY_CANCELLED,
    LICENSE_ALREADY_EXPIRED,
    LICENSE_NOT_MATCH,
    TOO_MANY_REQUESTS,
    MAX_TRAY_REACHED,
    SIGNATURE_INVALID

}