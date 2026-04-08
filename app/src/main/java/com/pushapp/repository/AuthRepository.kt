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

    // Логин через username: внутри используем username@pushapp.app как email
    suspend fun register(username: String, password: String): Result<User> {
        return try {
            val email = "${username.lowercase()}@pushapp.app"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: error("UID не получен")
            val user = User(uid = uid, username = username)
            db.collection("users").document(uid).set(
                mapOf("uid" to uid, "username" to username)
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
            val doc = db.collection("users").document(uid).get().await()
            val user = User(uid = uid, username = doc.getString("username") ?: username)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return try {
            val doc = db.collection("users").document(uid).get().await()
            User(uid = uid, username = doc.getString("username") ?: "")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            db.collection("users").get().await().documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: return@mapNotNull null
                val username = doc.getString("username") ?: return@mapNotNull null
                User(uid = uid, username = username)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
