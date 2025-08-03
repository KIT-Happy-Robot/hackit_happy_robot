package com.example.test_aprication.model

data class Emotion(
    val emotion: String,
    val level: Int,
    val senderId: String? = null // senderIdはnullを許容する
)