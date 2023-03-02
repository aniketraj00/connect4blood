package com.aniket.connect4blood.models

import com.aniket.connect4blood.utils.Utils
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User (
    var name: String? = null,
    var email: String? = null,
    var phoneNo: String? = null,
    var image: String? = null,
    var dateOfBirth: String? = null,
    var gender: String? = null,
    var isEligible: Boolean? = null,
    var isActive: Boolean? = null,
    val bloodGroup: String? = null,
    val bloodRequestHistory: Map<String, String?>? = null,
    val bloodDonationHistory: Map<String, String?>? = null
):java.io.Serializable {
    @Exclude
    fun toMap(): Map<String, Any?> {
        val age = Utils.getInstance().calcAge(dateOfBirth.toString()).toString()
        return mapOf(
            "name" to name,
            "age" to age,
            "gender" to gender,
            "image" to image,
            "phoneNo" to phoneNo
        )
    }
}