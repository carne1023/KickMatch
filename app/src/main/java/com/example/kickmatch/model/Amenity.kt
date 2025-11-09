package com.example.kickmatch.model

data class Amenity(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    val isAvailable: Boolean = false
) {
    companion object {

        fun getCommonAmenities(): List<Amenity> {
            return listOf(
                Amenity(
                    id = "parking",
                    name = "Parqueadero",
                    iconName = "ic_parking",
                    isAvailable = false
                ),
                Amenity(
                    id = "lighting",
                    name = "Iluminación",
                    iconName = "ic_light",
                    isAvailable = false
                ),
                Amenity(
                    id = "showers",
                    name = "Duchas",
                    iconName = "ic_shower",
                    isAvailable = false
                ),
                Amenity(
                    id = "lockers",
                    name = "Vestidores",
                    iconName = "ic_locker",
                    isAvailable = false
                ),
                Amenity(
                    id = "bleachers",
                    name = "Graderías",
                    iconName = "ic_bleachers",
                    isAvailable = false
                ),
                Amenity(
                    id = "cafeteria",
                    name = "Cafetería",
                    iconName = "ic_restaurant",
                    isAvailable = false
                ),
                Amenity(
                    id = "wifi",
                    name = "WiFi",
                    iconName = "ic_wifi",
                    isAvailable = false
                ),
                Amenity(
                    id = "security",
                    name = "Seguridad",
                    iconName = "ic_security",
                    isAvailable = false
                ),
                Amenity(
                    id = "scoreboard",
                    name = "Marcador",
                    iconName = "ic_scoreboard",
                    isAvailable = false
                ),
                Amenity(
                    id = "first_aid",
                    name = "Primeros auxilios",
                    iconName = "ic_medical",
                    isAvailable = false
                )
            )
        }
    }
}