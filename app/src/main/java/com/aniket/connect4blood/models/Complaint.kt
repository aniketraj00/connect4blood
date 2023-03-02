package com.aniket.connect4blood.models

import com.google.firebase.database.IgnoreExtraProperties
@IgnoreExtraProperties
data class Complaint (
    val email: String? = null,
    val phoneNo: String? = null,
    val subject: String? = null,
    val message: String? = null,
    val status: ComplaintStatus? = null,
    val dateTime: String? = null
)

enum class ComplaintStatus {
    PENDING,
    RESOLVED
}