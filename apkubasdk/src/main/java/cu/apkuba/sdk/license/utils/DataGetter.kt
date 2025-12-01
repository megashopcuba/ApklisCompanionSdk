package cu.apkuba.sdk.license.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import androidx.core.net.toUri
import com.orhanobut.logger.Logger
import cu.apkuba.sdk.license.models.AccountData
import cu.uci.android.apklis_license_validator.ApklisDataGetter

class DataGetter {

    companion object {
        private const val APKLIS_PACKAGE = "cu.uci.android.apklis"
        private const val AUTHORITY = "$APKLIS_PACKAGE.ApklisLicenseProvider"
        private val CONTENT_URI = "content://$AUTHORITY/account_data".toUri()
        private val apklisDataGetter by lazy { ApklisDataGetter }

        // Column names
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_DEVICE_ID = "device_id"
        private const val COLUMN_ACCESS_TOKEN = "access_token"
        private const val COLUMN_CODE = "code"


        /**
         * Accede al ContentProvider de APKlis de forma segura
         */
        fun getApklisAccountData(
            context: Context
        ): AccountData? {
            try {


                apklisDataGetter.isApklisDataAvailable(context)


                // Intentar acceder con FLAG_GRANT_READ_URI_PERMISSION
                val cursor = context.contentResolver.query(
                    CONTENT_URI,
                    null,
                    null,
                    null,
                    null
                )
                if (BuildConfig.DEBUG) Logger.d("Info: %s", "querying_apklis_data")


                if (cursor == null) {
                    if (BuildConfig.DEBUG) Logger.e(
                        "Info: %s",
                        "cursor_is_null_provider_not_available"
                    )

                    return null
                }

                if (BuildConfig.DEBUG) Logger.d(
                    "Info: %s",
                    "cursor_returned_with_rows ${cursor.count}"
                )


                val data = extractData(cursor)
                if (BuildConfig.DEBUG) Logger.d("Data: %s", data)
                cursor.close()
                return data

            } catch (e: SecurityException) {
                e.printStackTrace()
                // Si falla, intentar con grantUriPermission
                if (tryGrantUriPermission(context)) {
                    try {

                        val cursor = context.contentResolver.query(
                            CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                        )
                        val data = extractData(cursor)

                        if (BuildConfig.DEBUG) Logger.d("Data: %s", data)
                        cursor?.close()
                        return data
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                } else {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * Helper method to safely get string from cursor
         */
        private fun Cursor.getString(columnName: String): String? {
            val columnIndex = getColumnIndex(columnName)
            return if (columnIndex >= 0) getString(columnIndex) else null
        }

        /**
         * Checks if the Apklis app is installed and the provider is available
         * @param context The application context
         * @return true if provider is available, false otherwise
         */
        fun isApklisDataAvailable(context: Context): Boolean {
            return try {
//                context.grantUriPermission(
//                    APKLIS_PACKAGE,
//                    CONTENT_URI,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION
//                )
                context.contentResolver.takePersistableUriPermission(
                    CONTENT_URI,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val cursor = context.contentResolver.query(
                    CONTENT_URI,
                    arrayOf(COLUMN_USERNAME), // minimal projection
                    null, null, null
                )

                cursor?.use { true } == true

            } catch (e: Exception) {
                Logger.e("Info: %s", e.message)
                false
            }
        }


        private fun extractData(cursor: Cursor?): AccountData? {
            return if (cursor?.moveToFirst() == true) {
                val username = cursor.getString(COLUMN_USERNAME)
                val deviceId = cursor.getString(COLUMN_DEVICE_ID)
                val accessToken = cursor.getString(COLUMN_ACCESS_TOKEN)
                val code = cursor.getString(COLUMN_CODE)
                AccountData(username, deviceId, accessToken, code)

            } else {
                if (BuildConfig.DEBUG) Logger.e("Info: %s", "no_account_data_found")
                null
            }
        }

        /**
         * Intenta usar grantUriPermission como alternativa
         */
        fun tryGrantUriPermission(context: Context): Boolean {
            return try {

                context.grantUriPermission(
                    APKLIS_PACKAGE,
                    CONTENT_URI,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

