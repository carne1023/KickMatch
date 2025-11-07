package com.example.kickmatch.model

data class Field(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val type: String = "", // "Fútbol 5", "Fútbol 7", "Fútbol 11"
    val surface: String = "", // "Sintética", "Natural", "Cemento"
    val pricePerHour: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var distance: Float = 0f, // en kilómetros
    val rating: Float = 0f,
    val imageUrl: String = "",
    val hasParking: Boolean = false,
    val hasLighting: Boolean = false,
    val hasShowers: Boolean = false,
    val description: String = "",
    val phoneNumber: String = "",
    val openingHours: String = "8:00 - 22:00"
)