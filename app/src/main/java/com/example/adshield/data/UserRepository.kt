package com.example.adshield.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.tasks.await
import android.util.Log

object UserRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Reactive user state
    private val _user = kotlinx.coroutines.flow.MutableStateFlow<FirebaseUser?>(null)
    val user: kotlinx.coroutines.flow.StateFlow<FirebaseUser?> = _user

    init {
        // Initialize state with current user and listen for changes
        _user.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }
    }



    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                // Create user doc if new
                createOrUpdateUser()
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after sign in"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Firebase sign in failed", e)
            Result.failure(e)
        }
    }



    suspend fun fetchUserAccess(): UserAccess {
        val user = auth.currentUser ?: return UserAccess(UserAccessState.FREE)
        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            val stateName = doc.getString("userAccessState") ?: UserAccessState.FREE.name
            val trialEndsAt = if (doc.contains("trialEndsAt")) doc.getLong("trialEndsAt") else null
            val premiumExpiresAt = if (doc.contains("premiumExpiresAt")) doc.getLong("premiumExpiresAt") else null

            val state = try {
                UserAccessState.valueOf(stateName)
            } catch (_: Exception) { 
                UserAccessState.FREE
            }
            UserAccess(state, trialEndsAt, premiumExpiresAt)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user access", e)
            UserAccess(UserAccessState.FREE)
        }
    }

    suspend fun updateUserAccess(access: UserAccess) {
        val user = auth.currentUser ?: return
        try {
            val data = hashMapOf(
                "userAccessState" to access.state.name,
                "trialEndsAt" to access.trialEndsAt,
                "premiumExpiresAt" to access.premiumExpiresAt,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            // Remove legacy field to avoid confusion in future
            val updates = data as MutableMap<String, Any?>
            updates["isPremium"] = com.google.firebase.firestore.FieldValue.delete()
            
            db.collection("users").document(user.uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user access", e)
        }
    }

    suspend fun createOrUpdateUser() {
        val user = auth.currentUser ?: return
        val userMap = hashMapOf(
            "email" to (user.email ?: ""),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )

        try {
            // Set with merge to avoid overwriting existing fields like isPremium
            db.collection("users").document(user.uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user doc", e)
        }
    }



    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }
}
