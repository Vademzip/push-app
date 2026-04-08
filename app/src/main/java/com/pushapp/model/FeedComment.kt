package com.pushapp.model

data class FeedComment(
    val id: String = "",
    val workoutId: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
