package com.pushapp.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pushapp.model.FriendEntry
import com.pushapp.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendRepository {
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val myUid get() = auth.currentUser?.uid ?: ""
    private val usersCol get() = db.collection("users")
    private fun friendsCol(uid: String) = usersCol.document(uid).collection("friends")

    /** Поиск по префиксу username (без учёта регистра). Исключает самого себя. */
    suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return try {
            usersCol
                .whereGreaterThanOrEqualTo("usernameLower", q)
                .whereLessThanOrEqualTo("usernameLower", q + "")
                .limit(20)
                .get().await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
                .filter { it.uid != myUid }
        } catch (e: Exception) { emptyList() }
    }

    /** Отправить заявку в друзья. Атомарно пишет в обе стороны. */
    suspend fun sendRequest(toUid: String, toUsername: String, toDisplayName: String): Result<Unit> {
        return try {
            if (friendsCol(myUid).document(toUid).get().await().exists()) {
                return Result.success(Unit)
            }
            val myDoc = usersCol.document(myUid).get().await()
            val myUsername = myDoc.getString("username") ?: ""
            val myDisplayName = myDoc.getString("displayName") ?: ""
            val now = System.currentTimeMillis()
            db.batch().apply {
                set(friendsCol(myUid).document(toUid), FriendEntry(
                    uid = toUid, username = toUsername, displayName = toDisplayName,
                    status = "pending", sent = true, createdAt = now
                ))
                set(friendsCol(toUid).document(myUid), FriendEntry(
                    uid = myUid, username = myUsername, displayName = myDisplayName,
                    status = "pending", sent = false, createdAt = now
                ))
            }.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Принять входящую заявку. */
    suspend fun acceptRequest(fromUid: String): Result<Unit> {
        return try {
            db.batch().apply {
                update(friendsCol(myUid).document(fromUid), "status", "accepted")
                update(friendsCol(fromUid).document(myUid), "status", "accepted")
            }.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Отклонить заявку или удалить из друзей. Атомарно удаляет обе стороны. */
    suspend fun declineOrRemove(otherUid: String): Result<Unit> {
        return try {
            db.batch().apply {
                delete(friendsCol(myUid).document(otherUid))
                delete(friendsCol(otherUid).document(myUid))
            }.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Единый realtime-поток всех записей из subcollection friends текущего юзера. */
    fun allFriendEntriesFlow(): Flow<List<FriendEntry>> = callbackFlow {
        val listener = friendsCol(myUid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(FriendEntry::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /** UID всех принятых друзей (для фильтрации ленты). */
    suspend fun getFriendUids(): List<String> {
        return try {
            friendsCol(myUid)
                .whereEqualTo("status", "accepted")
                .get().await()
                .documents.mapNotNull { it.getString("uid") }
        } catch (e: Exception) { emptyList() }
    }

    /** Текущий статус отношений с конкретным пользователем. */
    suspend fun getFriendStatus(otherUid: String): FriendEntry? {
        return try {
            friendsCol(myUid).document(otherUid).get().await()
                .toObject(FriendEntry::class.java)
        } catch (e: Exception) { null }
    }
}
