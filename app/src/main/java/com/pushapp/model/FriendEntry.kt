package com.pushapp.model

data class FriendEntry(
    var uid: String = "",
    var username: String = "",
    var status: String = "",    // "pending" | "accepted"
    var sent: Boolean = false,  // true = текущий юзер отправил заявку
    var createdAt: Long = 0
)
