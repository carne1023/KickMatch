package com.example.kickmatch.utils

import com.example.kickmatch.R

object AmenitiesIcons {

    fun getIconForAmenity(id: String): Int {
        return when (id.lowercase()) {
            "parqueadero" -> R.drawable.ic_parking
            "iluminacion" -> R.drawable.ic_light
            "duchas" -> R.drawable.ic_shower
            "vestidores" -> R.drawable.ic_locker
            "graderias" -> R.drawable.ic_bleacher
            "cafeteria" -> R.drawable.ic_cafeteria
            "wifi" -> R.drawable.ic_wifi
            "seguridad" -> R.drawable.ic_security
            "marcador" -> R.drawable.ic_scoreboard
            "primeros_auxilios" -> R.drawable.ic_medical
            else -> R.drawable.ic_launcher_round
        }
    }
}
