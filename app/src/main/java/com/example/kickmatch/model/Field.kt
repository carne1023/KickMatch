package com.example.kickmatch.model

import java.io.Serializable

data class Field(
    val id: String = "",
    val adminId: String = "",
    val name: String = "",
    val address: String = "",
    val type: String = "",
    val surface: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var distance: Float = 0f,
    val pricePerHour: Double = 0.0,
    val rating: Float = 0f,
    val imageUrl: String = "",
    val hasParking: Boolean = false,
    val hasLighting: Boolean = false,
    val hasShowers: Boolean = false,
    val description: String = "",
    val phoneNumber: String = "",
    val openingHours: String = "",
    val totalRatings: Int = 0,
    val isActive: Boolean = true,
    val photos: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
): Serializable


