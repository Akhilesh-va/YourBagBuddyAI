package com.example.yourbagbuddy.domain.repository

import android.app.Activity
import com.example.yourbagbuddy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun getCurrentUser(): User?

    // Phone auth
    suspend fun sendPhoneOtp(phoneNumber: String, activity: Activity): Result<String>
    suspend fun verifyPhoneOtp(verificationId: String, code: String): Result<User>

    // Profile in Firestore (signup / complete profile)
    suspend fun saveUserProfile(userId: String, displayName: String, email: String?, phoneNumber: String): Result<Unit>

    // Google sign-in (Credential Manager flow; use activity for the sign-in UI)
    suspend fun signInWithGoogle(activity: Activity): Result<User>

    // Email / password
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String): Result<User>

    // Email link (passwordless)
    suspend fun sendSignInLinkToEmail(email: String): Result<Unit>
    suspend fun signInWithEmailLink(email: String, link: String): Result<User>
    fun setPendingEmailForLink(email: String)
    fun getPendingEmailForLink(): String?
    fun setPendingEmailLink(link: String)
    fun getAndClearPendingEmailLink(): String?
    /** If the given URL is a Firebase email sign-in link, store it and return true. Call from Activity when handling intent.data. */
    fun storePendingEmailLinkIfSignInLink(link: String): Boolean

    suspend fun signOut(): Result<Unit>

    /**
     * Retrieves the Firebase ID token for the currently signed-in user.
     * This token should be sent in the Authorization header when calling the backend API.
     * Returns null if no user is signed in or if token retrieval fails.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String?
}
