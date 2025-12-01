package cu.apkuba.sdk.license.models

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class LicenseResponse(
    @SerializedName("expire_in") val expireIn: String,
    @SerializedName("license") val license: String
) {
    val jsonString: String
        get() = """{"license": "$license", "expire_in": "$expireIn"}"""


    companion object {
        val type: Type = object : TypeToken<LicenseResponse>() {}.type
        val listType: Type = object : TypeToken<List<LicenseResponse>>() {}.type

    }
}

enum class LicenseStatus {
    PENDING, PAYED, EXPIRED, CANCELLED, INVALID, ERROR, SIGNATURE_INVALID, NOT_PAYED
}