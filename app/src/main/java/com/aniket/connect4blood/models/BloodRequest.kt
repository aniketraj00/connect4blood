package com.aniket.connect4blood.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BloodRequest (
    val requesterId: String? = null,
    val requestDateTime: String? = null,
    val recipientName: String? = null,
    val bloodGroup: String? = null,
    val unitsRequired: Int? = null,
    val hospitalName: String? = null,
    val hospitalAddress: String? = null,
    val location: Map<String, Double?>? = null,
    val searchRadius: Int? = null,
    val status: BloodRequestStatus? = null,
    val donors: Map<String, Any?>? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "requesterId" to requesterId,
            "requestDateTime" to requestDateTime,
            "recipientName" to recipientName,
            "bloodGroup" to bloodGroup,
            "unitsRequired" to unitsRequired,
            "hospitalName" to hospitalName,
            "hospitalAddress" to hospitalAddress,
            "location" to location,
            "searchRadius" to searchRadius,
            "status" to status,
            "donors" to donors
        )
    }
}

enum class BloodRequestStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}