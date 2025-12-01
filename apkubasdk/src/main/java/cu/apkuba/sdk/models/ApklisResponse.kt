/*
 * ApKuba SDK
 * Copyright (c) 2025 VirtualShopRest
 *
 * This library provides easy integration with Apklis and ApKuba
 * payment verification systems for Android applications.
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * GitHub: https://github.com/virtualShopRest/ApKubaSdk
 *
 */


package cu.apkuba.sdk.models

data class ApklisResponse(
    var userName: String? = null,
    var status: PaymentStatus = PaymentStatus.NOT_PAYED,
    var message: Exception? = null
) {

    fun isValid(): Boolean {
        return PaymentStatus.PAYED == status
    }

    override fun toString(): String {
        return "User: $userName\n" +
                "Status: $status\n" +
                "Message: ${message?.message}"
    }


}

data class ApklisLicenseResponse(
    var licenseUuid: String? = null,
    var uuid: String? = null,
    var expiredIn: String? = null,
    var status: LicenseStatus = LicenseStatus.PENDING,
    var message: Exception? = null
) {

    fun isValid(): Boolean {
        return LicenseStatus.PAYED == status
    }

    override fun toString(): String {
        return "License Uuid: $licenseUuid \n" +
                "Uuid: $uuid \n" +
                "Expired In: $expiredIn \n" +
                "Status: $status \n" +
                "Message: ${message?.message}"
    }

}

enum class PaymentStatus {
    NOT_PAYED, PAYED, ERROR, EXCEPTION
}

enum class LicenseStatus {
    PENDING, PAYED, EXPIRED, CANCELLED, INVALID, ERROR, SIGNATURE_INVALID, NOT_PAYED
}
