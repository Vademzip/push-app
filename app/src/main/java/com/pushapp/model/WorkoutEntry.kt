package com.pushapp.model

data class WorkoutEntry(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val date: String = "",      // формат: "yyyy-MM-dd"
    val pushups: Int = 0,
    val squats: Int = 0,
    val pullups: Int = 0,
    val abs: Int = 0,
    val comment: String = "",
    val skipped: Boolean = false,
    val timestamp: Long = 0L
)
