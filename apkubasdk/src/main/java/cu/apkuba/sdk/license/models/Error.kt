package cu.apkuba.sdk.license.models

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@Keep
data class Error(
    @SerializedName("error") val error: String?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("code") val code: String?
) {
    companion object {
        val type: Type = object : TypeToken<Error>() {}.type

    }
}

