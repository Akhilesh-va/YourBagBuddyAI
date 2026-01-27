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

    // Legacy / optional
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
}
