package com.example.f_rizzly.auth

import com.google.android.gms.common.api.ApiException
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _authError = MutableLiveData<String?>()
    val authError: LiveData<String?> = _authError

    init {
        _currentUser.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("941997129015-4bepdu3uaguhm4lv1eurfqe40qmqp7hs.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application.applicationContext, gso)
    }

    fun signInWithGoogle(launcher: ActivityResultLauncher<Intent>) {
        android.util.Log.d("AuthViewModel", "signInWithGoogle called")
        _authError.value = null
        try {
            val signInIntent = googleSignInClient.signInIntent
            android.util.Log.d("AuthViewModel", "Launching sign-in intent")
            launcher.launch(signInIntent)
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to launch sign-in", e)
            _authError.value = "Failed to launch sign-in: ${e.message}"
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        android.util.Log.d("AuthViewModel", "handleGoogleSignInResult called, data=$data")
        viewModelScope.launch {
            if (data == null) {
                android.util.Log.e("AuthViewModel", "No data received from sign-in")
                _authError.value = "Sign-in cancelled - no data received"
                return@launch
            }
            
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            android.util.Log.d("AuthViewModel", "Got task from intent, isSuccessful=${task.isSuccessful}")
            
            try {
                val account = task.getResult(ApiException::class.java)!!
                android.util.Log.d("AuthViewModel", "Got account successfully")
                
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                android.util.Log.d("AuthViewModel", "Created Firebase credential")
                
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        android.util.Log.d("AuthViewModel", "Firebase auth complete, success=${authTask.isSuccessful}")
                        if (authTask.isSuccessful) {
                            _currentUser.value = auth.currentUser
                            android.util.Log.d("AuthViewModel", "User signed in successfully")
                            
                            // Register FCM token after successful sign-in
                            registerFCMToken()
                        } else {
                            android.util.Log.e("AuthViewModel", "Firebase auth failed", authTask.exception)
                            _authError.value = "Firebase auth failed: ${authTask.exception?.message}"
                        }
                    }
            } catch (e: ApiException) {
                android.util.Log.e("AuthViewModel", "ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
                _authError.value = "Sign-in error (code ${e.statusCode}): ${e.message}"
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Unexpected error", e)
                _authError.value = "Unexpected error: ${e.message}"
            }
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
        _currentUser.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }
    
    private fun registerFCMToken() {
        android.util.Log.d("FCM", "registerFCMToken() called")
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            android.util.Log.d("FCM", "Got FCM token: ${token.take(20)}...")
            val userId = auth.currentUser?.uid
            android.util.Log.d("FCM", "User ID: $userId")
            if (userId != null) {
                // Get phone number from SIM card
                val simPhoneNumber = try {
                    val context = getApplication<android.app.Application>()
                    val telephonyManager = context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                    telephonyManager.line1Number ?: ""
                } catch (e: Exception) {
                    ""
                }
                
                // Get device info
                val deviceInfo = mapOf(
                    "model" to android.os.Build.MODEL,
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "androidVersion" to android.os.Build.VERSION.RELEASE,
                    "sdkInt" to android.os.Build.VERSION.SDK_INT,
                    "brand" to android.os.Build.BRAND,
                    "device" to android.os.Build.DEVICE
                )
                
                val userData = mapOf(
                    "fcmToken" to token,
                    "deviceInfo" to deviceInfo,
                    "appVersion" to "1.0.0",
                    "lastLogin" to com.google.firebase.Timestamp.now(),
                    "email" to (auth.currentUser?.email ?: ""),
                    "displayName" to (auth.currentUser?.displayName ?: ""),
                    "phone" to simPhoneNumber.ifEmpty { auth.currentUser?.phoneNumber ?: "" }
                )
                
                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                
                userRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        android.util.Log.d("FCM", "User data saved with device info")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FCM", "Failed to save token: ${e.message}")
                    }
            }
        }
    }
}
