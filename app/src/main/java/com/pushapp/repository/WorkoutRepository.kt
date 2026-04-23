package com.pushapp.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import com.pushapp.model.FeedComment
import com.pushapp.model.WorkoutEntry
import kotlinx.coroutines.tasks.await

class WorkoutRepository {
    private val collection = FirebaseFirestore.getInstance().collection("workouts")

    private fun commentsCollection(workoutId: String) =
        collection.document(workoutId).collection("comments")

    suspend fun saveWorkout(entry: WorkoutEntry): Result<Unit> {
        return try {
            val existing = collection
                .whereEqualTo("userId", entry.userId)
                .whereEqualTo("date", entry.date)
                .get().await()

            if (existing.documents.isNotEmpty()) {
                val docId = existing.documents[0].id
                collection.document(docId).set(entry.copy(id = docId)).await()
            } else {
                val docRef = collection.document()
                collection.document(docRef.id).set(entry.copy(id = docRef.id)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTodayWorkout(userId: String, today: String): WorkoutEntry? {
        return try {
            collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .get().await()
                .documents.firstOrNull()
                ?.toObject(WorkoutEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeed(): List<WorkoutEntry> {
        return try {
            collection
                .orderBy("timestamp", Direction.DESCENDING)
                .limit(50)
                .get().await()
                .documents.mapNotNull { it.toObject(WorkoutEntry::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getComments(workoutId: String): List<FeedComment> {
        return try {
            commentsCollection(workoutId)
                .orderBy("timestamp", Direction.ASCENDING)
                .get().await()
                .documents.mapNotNull { it.toObject(FeedComment::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComment(workoutId: String, comment: FeedComment): Result<Unit> {
        return try {
            val ref = commentsCollection(workoutId).document()
            ref.set(comment.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserWorkouts(userId: String): List<WorkoutEntry> {
        return try {
            collection
                .whereEqualTo("userId", userId)
                .get().await()
                .documents.mapNotNull { it.toObject(WorkoutEntry::class.java) }
                .sortedByDescending { it.timestamp }
                .take(20)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Без orderBy — составной индекс не нужен. Сортировка на клиенте.
    suspend fun getUserWorkoutsForPeriod(
        userId: String,
        fromDate: String,
        toDate: String
    ): List<WorkoutEntry> {
        return try {
            collection
                .whereEqualTo("userId", userId)
                .get().await()
                .documents.mapNotNull { it.toObject(WorkoutEntry::class.java) }
                .filter { it.date >= fromDate && it.date <= toDate }
                .sortedBy { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
