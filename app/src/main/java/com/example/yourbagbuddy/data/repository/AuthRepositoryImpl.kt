package com.example.yourbagbuddy.data.repository

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.CancellationSignal
import com.example.yourbagbuddy.BuildConfig
import com.example.yourbagbuddy.domain.model.User
import com.example.yourbagbuddy.domain.repository.AuthRepository
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection
        get() = firestore.collection(USERS_COLLECTION)

    private val authPrefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)

    override val currentUser: Flow<User?>
        get() = callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val fbUser = auth.currentUser
                trySend(if (fbUser != null) fbUser.toDomainMinimal() else null)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(listener)
            }
        }

    override suspend fun getCurrentUser(): User? {
        val fbUser = firebaseAuth.currentUser ?: return null
        return try {
            loadUserWithProfile(fbUser)
        } catch (e: Exception) {
            // If Firestore is unavailable (e.g., offline or DB not created),
            // fall back to a minimal user from FirebaseAuth instead of crashing.
            fbUser.toDomainMinimal()
        }
    }

    override suspend fun sendPhoneOtp(phoneNumber: String, activity: Activity): Result<String> {
        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (cont.isActive) cont.resume(Result.failure(Exception("Use manual OTP flow"))) { }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    if (cont.isActive) cont.resume(Result.failure(e)) { }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    if (cont.isActive) cont.resume(Result.success(verificationId)) { }
                }
            }
            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(normalizePhone(phoneNumber))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyPhoneOtp(verificationId: String, code: String): Result<User> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user?.let { loadUserWithProfile(it) }
                ?: return Result.failure(Exception("Sign in failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        phoneNumber: String
    ): Result<Unit> {
        return try {
            val doc = mapOf(
                KEY_DISPLAY_NAME to displayName.trim(),
                KEY_EMAIL to (email?.trim().takeIf { !it.isNullOrEmpty() }),
                KEY_PHONE to phoneNumber.trim(),
                KEY_UPDATED_AT to Timestamp.now()
            )
            usersCollection.document(userId).set(doc).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.let { loadUserWithProfile(it) }
                ?: return Result.failure(Exception("Sign in failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.let { loadUserWithProfile(it) }
                ?: return Result.failure(Exception("Sign up failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<User> {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Add GOOGLE_WEB_CLIENT_ID to local.properties. " +
                        "Get it from Firebase Console > Project settings > Your apps > Web client, or Google Cloud > Credentials."
                )
            )
        }
        return runCatching {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = signInWithGoogleAwait(credentialManager, activity, request)
            val credential = response.credential
            val idToken = when {
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                else -> throw IllegalStateException("Unexpected credential type")
            }
            val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(fbCredential).await()
            result.user?.let { loadUserWithProfile(it) }
                ?: throw Exception("Google sign in failed")
        }
    }

    private suspend fun signInWithGoogleAwait(
        credentialManager: CredentialManager,
        activity: Activity,
        request: GetCredentialRequest
    ): GetCredentialResponse = suspendCancellableCoroutine { cont ->
        val cancellationSignal = CancellationSignal()
        cont.invokeOnCancellation { cancellationSignal.cancel() }
        credentialManager.getCredentialAsync(
            activity,
            request,
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            object : CredentialManagerCallback<GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    if (cont.isActive) cont.resume(result) {}
                }
                override fun onError(error: androidx.credentials.exceptions.GetCredentialException) {
                    if (cont.isActive) cont.resumeWithException(error)
                }
            }
        )
    }

    override suspend fun sendSignInLinkToEmail(email: String): Result<Unit> {
        return try {
            // Firebase Hosting default domain; must be in Authorized domains in Firebase Console
            val continueUrl = "https://${FIREBASE_AUTH_LINK_HOST}/__/auth/links"
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl(continueUrl)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                    context.packageName,
                    true,  // installIfNotAvailable
                    null   // minimumVersion
                )
                .build()
            firebaseAuth.sendSignInLinkToEmail(email.trim(), actionCodeSettings).await()
            setPendingEmailForLink(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailLink(email: String, link: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailLink(email.trim(), link.trim()).await()
            val user = result.user?.let { loadUserWithProfile(it) }
                ?: return Result.failure(Exception("Sign in failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setPendingEmailForLink(email: String) {
        authPrefs.edit().putString(KEY_PENDING_EMAIL_LINK, email).apply()
    }

    override fun getPendingEmailForLink(): String? {
        return authPrefs.getString(KEY_PENDING_EMAIL_LINK, null)?.takeIf { it.isNotBlank() }
    }

    override fun setPendingEmailLink(link: String) {
        authPrefs.edit().putString(KEY_PENDING_LINK_URL, link).apply()
    }

    override fun getAndClearPendingEmailLink(): String? {
        val link = authPrefs.getString(KEY_PENDING_LINK_URL, null)?.takeIf { it.isNotBlank() }
        if (link != null) authPrefs.edit().remove(KEY_PENDING_LINK_URL).apply()
        return link
    }

    override fun storePendingEmailLinkIfSignInLink(link: String): Boolean {
        return if (firebaseAuth.isSignInWithEmailLink(link)) {
            setPendingEmailLink(link)
            true
        } else {
            false
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadUserWithProfile(fbUser: FirebaseUser): User {
        return try {
            val snapshot = usersCollection.document(fbUser.uid).get().await()
            val name = snapshot.getString(KEY_DISPLAY_NAME)
            val email = snapshot.getString(KEY_EMAIL) ?: fbUser.email
            val phone = snapshot.getString(KEY_PHONE) ?: fbUser.phoneNumber
            val updatedAt = snapshot.getTimestamp(KEY_UPDATED_AT)?.toDate()?.time
            User(
                id = fbUser.uid,
                email = email,
                displayName = name ?: fbUser.displayName,
                phoneNumber = phone,
                createdAtMs = updatedAt
            )
        } catch (e: Exception) {
            // If Firestore profile fetch fails, degrade gracefully to minimal info.
            fbUser.toDomainMinimal()
        }
    }

    private fun FirebaseUser.toDomainMinimal(): User = User(
        id = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber,
        createdAtMs = null
    )

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.isEmpty()) raw.trim() else if (raw.trim().startsWith("+")) raw.trim() else "+$digits"
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phoneNumber"
        private const val KEY_UPDATED_AT = "updatedAt"
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_PENDING_EMAIL_LINK = "pending_email_for_link"
        private const val KEY_PENDING_LINK_URL = "pending_email_link_url"
        private const val FIREBASE_AUTH_LINK_HOST = "yourbagbuddy-ai.firebaseapp.com"
    }
}
