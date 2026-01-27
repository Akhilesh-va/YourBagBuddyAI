package com.example.yourbagbuddy.domain.model

data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String? = null,
    val createdAtMs: Long? = null
) {
    /** True if the user has completed signup (profile saved in Firestore). */
    val hasCompleteProfile: Boolean
        get() = !displayName.isNullOrBlank()
}
