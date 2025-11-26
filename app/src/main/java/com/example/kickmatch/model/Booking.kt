package com.example.kickmatch.model

data class Booking(
    val id: String = "",
    val fieldId: String = "",
    val fieldName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val date: Long = 0L, // Timestamp de la fecha
    val startTime: String = "", // Formato: 08:00
    val endTime: String = "",
    val duration: Int = 1, // Horas
    val totalPrice: Double = 0.0,
    val status: BookingStatus = BookingStatus.PENDING,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class BookingStatus {
    PENDING,    // Pendiente de confirmación
    CONFIRMED,  // Confirmada
    CANCELLED,  // Cancelada
    COMPLETED   // Completada
}

data class TimeSlot(
    val time: String,        // "08:00"
    val isAvailable: Boolean = true,
    val bookingId: String? = null
)

// slots de tiempo
object TimeSlotHelper {
    fun generateTimeSlots(): List<String> {
        val slots = mutableListOf<String>()
        for (hour in 6..22) { // 6 AM a 10 PM
            slots.add(String.format("%02d:00", hour))
        }
        return slots
    }

    fun getEndTime(startTime: String, duration: Int): String {
        val hour = startTime.split(":")[0].toInt()
        val endHour = hour + duration
        return String.format("%02d:00", endHour)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "ES"))
        return sdf.format(java.util.Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale("es", "ES"))
        return sdf.format(java.util.Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es", "ES"))
        return sdf.format(java.util.Date(timestamp))
    }
}