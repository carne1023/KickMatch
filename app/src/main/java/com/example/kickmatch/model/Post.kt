package com.example.kickmatch.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val groupName: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var likes: Int = 0,
    val comments: Int = 0,
    var isLiked: Boolean = false
)