package com.example.kickmatch.utils

import com.example.kickmatch.R

object AmenitiesIcons {

    fun getIconForAmenity(id: String): Int {

        val key = id.lowercase().trim()

        return when (key) {

            "wifi"        -> R.drawable.ic_wifi
            "security"    -> R.drawable.ic_security
            "first_aid"   -> R.drawable.ic_medical
            "scoreboard"  -> R.drawable.ic_scoreboard
            "parking"     -> R.drawable.ic_parking
            "lighting"    -> R.drawable.ic_light
            "lockers"     -> R.drawable.ic_locker
            "cafeteria"   -> R.drawable.ic_cafeteria
            "bleachers"   -> R.drawable.ic_bleacher
            "showers"      -> R.drawable.ic_shower

            else -> R.drawable.ic_unknown_amenity
        }
    }
}
