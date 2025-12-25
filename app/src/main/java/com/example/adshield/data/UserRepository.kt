package com.example.adshield.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

object UserRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
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

    suspend fun isUserPremium(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            val isPremium = doc.getBoolean("isPremium") ?: false
            
            // TODO: Check expiration date logic here
            isPremium
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking premium status", e)
            false
        }
    }
    
    suspend fun updatePremiumStatus(isPremium: Boolean) {
        val user = auth.currentUser ?: return
        try {
            val data = hashMapOf("isPremium" to isPremium)
            db.collection("users").document(user.uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating premium status", e)
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
    
    fun signOut() {
        auth.signOut()
    }
}
