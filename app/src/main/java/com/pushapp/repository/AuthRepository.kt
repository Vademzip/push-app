package com.pushapp.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pushapp.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUserId: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun register(username: String, password: String, displayName: String): Result<User> {
        return try {
            val email = "${username.lowercase()}@pushapp.app"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: error("UID не получен")
            val user = User(uid = uid, username = username, usernameLower = username.lowercase(), displayName = displayName)
            db.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "username" to username,
                    "usernameLower" to username.lowercase(),
                    "displayName" to displayName
                )
            ).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val email = "${username.lowercase()}@pushapp.app"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: error("UID не получен")
            val ref = db.collection("users").document(uid)
            val doc = ref.get().await()
            val storedUsername = doc.getString("username") ?: username
            val displayName = doc.getString("displayName") ?: ""
            if (doc.getString("usernameLower") == null) {
                ref.update("usernameLower", storedUsername.lowercase()).await()
            }
            Result.success(User(
                uid = uid,
                username = storedUsername,
                usernameLower = storedUsername.lowercase(),
                displayName = displayName
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            User(
                uid = uid,
                username = doc.getString("username") ?: "",
                usernameLower = doc.getString("usernameLower") ?: "",
                displayName = doc.getString("displayName") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setDisplayName(name: String): Result<Unit> {
        return try {
            val uid = currentUserId ?: error("Не авторизован")
            db.collection("users").document(uid).update("displayName", name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            db.collection("users").get().await().documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: return@mapNotNull null
                val username = doc.getString("username") ?: return@mapNotNull null
                User(
                    uid = uid,
                    username = username,
                    displayName = doc.getString("displayName") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
