package cu.apkuba.sdk.license.models

import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val name: String,
    val uuid: String,
    val description: String,
    val icon: String,
    val price: Double,
    val duration: Int,
    val color: Int,
    val type: PlanType,
    val active: Boolean = false,
    val expireIn: String? = null

){
    fun isActive() {

    }
}

enum class PlanType {
    BRONZE,
    SILVER,
    GOLD,
    DIAMOND
}
