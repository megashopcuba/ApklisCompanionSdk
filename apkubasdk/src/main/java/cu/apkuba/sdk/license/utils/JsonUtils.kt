package cu.apkuba.sdk.license.utils

import com.google.gson.GsonBuilder
import java.lang.reflect.Type

data object JsonUtils {
    private val gson = GsonBuilder()
        .create()

    fun <T> parseString(toParse: String?, type: Type): T {
        return gson.fromJson(toParse, type)
    }

    fun toJson(objeto: Any, type: Type): String {
        return gson.toJson(objeto, type)

    }


}