package com.example.f_rizzly.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phones: List<String> = emptyList(),
    val photoUrl: String = "",
    val memberSince: String = ""
)

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
    object NotSignedIn : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState
    
    fun loadProfile(phonesFromSim: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _profileState.value = ProfileState.NotSignedIn
                    return@launch
                }
                
                val doc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val firestorePhone = doc.getString("phone") ?: ""
                val memberSince = doc.getTimestamp("createdAt")?.toDate()?.toString()?.take(10) ?: ""
                
                val allPhones = mutableListOf<String>()
                if (firestorePhone.isNotEmpty()) allPhones.add(firestorePhone)
                phonesFromSim.forEach { if (it.isNotEmpty() && !allPhones.contains(it)) allPhones.add(it) }
                
                val profile = UserProfile(
                    uid = currentUser.uid,
                    displayName = currentUser.displayName ?: "User",
                    email = currentUser.email ?: "",
                    phones = allPhones,
                    photoUrl = currentUser.photoUrl?.toString() ?: "",
                    memberSince = memberSince
                )
                
                _profileState.value = ProfileState.Success(profile)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
        _profileState.value = ProfileState.NotSignedIn
    }
}
