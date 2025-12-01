package cu.apkuba.sdk.license.models

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.MessageFormat
import kotlin.text.toDoubleOrNull

@Keep
class TransferMovil {
    @SerializedName("id_transaccion")
    var idTransaccion: String? = null
    @SerializedName("importe")
    private var importe: String? = null
    @SerializedName("moneda")
    var moneda: String? = null
    @SerializedName("numero_proveedor")
    var numeroProveedor: String? = null
    @SerializedName("version")
    var version = ""

    val isValid: Boolean
        get() = idTransaccion != null && importe != null && moneda != null && numeroProveedor != null

    val price: Double
        get() = importe?.toDoubleOrNull() ?: 0.0

    val url: String
        get() = MessageFormat.format(
            "transfermovil://tm_compra_en_linea/action?id_transaccion={0}&importe={1}&moneda={2}&numero_proveedor={3}",
            idTransaccion,
            importe,
            moneda,
            numeroProveedor
        )

    val jsonString: String
        get() = """{"id_transaccion": "$idTransaccion", "importe": "$importe", "moneda": "$moneda", "numero_proveedor": "$numeroProveedor", "version": "$version"}"""


    fun openLink(context: Context) {
        try {
            context.startActivity(Intent("android.intent.action.VIEW", url.toUri()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val type: Type = object : TypeToken<TransferMovil>() {}.type

    }
}