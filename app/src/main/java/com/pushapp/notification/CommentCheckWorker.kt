package com.pushapp.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.pushapp.repository.WorkoutRepository

class CommentCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!NotificationHelper.isCommentNotifEnabled(applicationContext)) return Result.success()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val lastCheck = NotificationHelper.getLastCommentCheck(applicationContext)
        val now = System.currentTimeMillis()

        val repo = WorkoutRepository()
        val workouts = repo.getUserWorkouts(uid)

        var newCount = 0
        var latestAuthor = ""

        for (workout in workouts) {
            val newComments = repo.getComments(workout.id)
                .filter { it.timestamp > lastCheck && it.userId != uid }
            if (newComments.isNotEmpty()) {
                newCount += newComments.size
                latestAuthor = newComments.last().username
            }
        }

        if (newCount > 0) {
            NotificationHelper.showCommentNotification(applicationContext, latestAuthor, newCount)
        }

        NotificationHelper.setLastCommentCheck(applicationContext, now)
        return Result.success()
    }
}
