package cu.apkuba.sdk.license.models

import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import java.lang.reflect.Type


@Serializable
data class ApklisUser(
    @SerializedName("id") var id: Int?,
    @SerializedName("avatar") var avatar: String?,
    @SerializedName("is_developer") var isDeveloper: Boolean?,
    @SerializedName("is_developer_active") var isDeveloperActive: Boolean?,
    @SerializedName("phone_number") var phoneNumber: String?,
    @SerializedName("code") var code: String?,
    @SerializedName("payed") var payed: List<String>?,
    @SerializedName("wishlist") var wishlist: List<String>?,
    @SerializedName("sha1") var sha1: String?,
    @SerializedName("seller") var seller: Seller?,
    @SerializedName("lastLogin") var lastLogin: String?,
    @SerializedName("is_superuser") var isSuperuser: Boolean?,
    @SerializedName("username") var username: String,
    @SerializedName("first_name") var firstName: String?,
    @SerializedName("last_name") var lastName: String?,
    @SerializedName("is_staff") var isStaff: Boolean?,
    @SerializedName("is_active") var isActive: Boolean?,
    @SerializedName("date_joined") var dateJoined: String?,
    @SerializedName("email") var email: String?,
    @SerializedName("beta_tester") var betaTester: Boolean?,
    @SerializedName("sales_admin") var salesAdmin: Boolean?,
    @SerializedName("is_deleted") var isDeleted: Boolean?,
    @SerializedName("theme") var theme: String?,
    @SerializedName("cid") var cid: String?,
    @SerializedName("province") var province: Int?,
    @SerializedName("country_code") var countryCode: String?,
    @SerializedName("municipality") var municipality: Int?,
    @SerializedName("favorite_apps") var favoriteApps: List<Int>?,
    @SerializedName("downloaded") var downloaded: List<Int>?
) {
    fun isValidUser(): Boolean {
        return !username.isNullOrBlank()

    }

    override fun toString(): String {
        return "ApklisUser(id=$id, avatar=$avatar, isDeveloper=$isDeveloper, isDeveloperActive=$isDeveloperActive, phoneNumber=$phoneNumber, code=$code, payed=$payed, wishlist=$wishlist, sha1=$sha1, seller=$seller, lastLogin=$lastLogin, isSuperuser=$isSuperuser, username='$username', firstName=$firstName, lastName=$lastName, isStaff=$isStaff, isActive=$isActive, dateJoined=$dateJoined, email=$email, betaTester=$betaTester, salesAdmin=$salesAdmin, isDeleted=$isDeleted, theme=$theme, cid=$cid, province=$province, countryCode=$countryCode, municipality=$municipality, favoriteApps=$favoriteApps, downloaded=$downloaded)"
    }

    companion object {
        var type: Type = object : TypeToken<ApklisUser?>() {}.type
    }
}


@Serializable
data class Seller(
    @SerializedName("account") val account: String?,
    @SerializedName("active") val active: Boolean?,
    @SerializedName("address") val address: String?,
    @SerializedName("bank_branch") val bankBranch: Int?,
    @SerializedName("bank_account") val bankAccount: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("cid") val cid: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("id") @PrimaryKey val id: Int,
    @SerializedName("joined") val joined: String?,
    @SerializedName("license") val license: String?,
    @SerializedName("municipality") val municipality: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("nit") val nit: String?,
    @SerializedName("province") val province: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("user") val user: String?,

    )