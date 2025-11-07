package com.example.kickmatch.api

import com.example.kickmatch.model.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GeoapifyService {

    companion object {
        private const val API_KEY = "d1adb8d21f854d3abc9e838fc18f890a"
        private const val BASE_URL = "https://api.geoapify.com/v2/places"
    }

    suspend fun searchSoccerFields(
        latitude: Double,
        longitude: Double,
        radius: Int = 15000
    ): Result<List<Field>> = withContext(Dispatchers.IO) {
        try {
            val categories =
                "sport.pitch,sport.stadium,leisure.park,activity.sport_club"
            val url = buildUrl(latitude, longitude, radius, categories)
            println("🌍 Fetch URL: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val fields = parseResponse(response, latitude, longitude)
                println("✅ Geoapify devolvió ${fields.size} lugares.")
                Result.success(fields)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("❌ Error ${responseCode}: $error")
                Result.failure(Exception("Error ${responseCode}: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByText(
        query: String,
        latitude: Double,
        longitude: Double
    ): Result<List<Field>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val categories =
                "sport.pitch,sport.stadium,leisure.sports_centre,leisure.park,activity.sport_club"
            val url = "$BASE_URL?" +
                    "categories=$categories&" +
                    "filter=circle:$longitude,$latitude,15000&" +
                    "bias=proximity:$longitude,$latitude&" +
                    "text=$encodedQuery&" +
                    "limit=20&" +
                    "apiKey=$API_KEY"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val fields = parseResponse(response, latitude, longitude)
                Result.success(fields)
            } else {
                Result.failure(Exception("Error $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUrl(
        latitude: Double,
        longitude: Double,
        radius: Int,
        categories: String
    ): String {
        return "$BASE_URL?" +
                "categories=$categories&" +
                "filter=circle:$longitude,$latitude,$radius&" +
                "bias=proximity:$longitude,$latitude&" +
                "limit=20&" +
                "apiKey=$API_KEY"
    }

    private fun parseResponse(
        jsonResponse: String,
        userLat: Double,
        userLon: Double
    ): List<Field> {
        val fields = mutableListOf<Field>()
        try {
            val json = JSONObject(jsonResponse)
            val features = json.getJSONArray("features")
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val geometry = feature.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val lon = coordinates.getDouble(0)
                val lat = coordinates.getDouble(1)
                val distance = calculateDistance(userLat, userLon, lat, lon)
                val name = properties.optString("name", "Cancha de fútbol")
                val address = buildAddress(properties)
                val placeId = properties.optString("place_id", "")
                val type = determineFieldType(properties)
                val surface = determineSurface(properties)
                val field = Field(
                    id = placeId,
                    name = name,
                    address = address,
                    type = type,
                    surface = surface,
                    pricePerHour = 0.0,
                    latitude = lat,
                    longitude = lon,
                    distance = distance,
                    rating = properties.optDouble("rating", 0.0).toFloat(),
                    imageUrl = "",
                    hasParking = properties.optJSONObject("datasource")?.optString("raw")?.contains("parking") ?: false,
                    hasLighting = properties.optJSONObject("datasource")?.optString("raw")?.contains("lit") ?: false,
                    hasShowers = false,
                    description = properties.optString("description", ""),
                    phoneNumber = properties.optString("contact:phone", ""),
                    openingHours = properties.optString("opening_hours", "")
                )
                fields.add(field)
            }
        } catch (_: Exception) {
        }
        return fields
    }

    private fun buildAddress(properties: JSONObject): String {
        val street = properties.optString("street", "")
        val housenumber = properties.optString("housenumber", "")
        val city = properties.optString("city", "")
        val country = properties.optString("country", "")
        val parts = mutableListOf<String>()
        if (street.isNotEmpty()) {
            if (housenumber.isNotEmpty()) parts.add("$street $housenumber") else parts.add(street)
        }
        if (city.isNotEmpty()) parts.add(city)
        if (country.isNotEmpty()) parts.add(country)
        return parts.joinToString(", ")
    }

    private fun determineFieldType(properties: JSONObject): String {
        val name = properties.optString("name", "").lowercase()
        val sport = properties.optString("sport", "").lowercase()
        return when {
            name.contains("futbol 11") || name.contains("fútbol 11") || sport.contains("11") -> "Fútbol 11"
            name.contains("futbol 7") || name.contains("fútbol 7") || sport.contains("7") -> "Fútbol 7"
            name.contains("futbol 5") || name.contains("fútbol 5") || sport.contains("5") -> "Fútbol 5"
            else -> "Fútbol 7"
        }
    }

    private fun determineSurface(properties: JSONObject): String {
        val surface = properties.optString("surface", "").lowercase()
        val description = properties.optString("description", "").lowercase()
        return when {
            surface.contains("artificial") || surface.contains("synthetic") ||
                    description.contains("sintética") || description.contains("sintetica") -> "Sintética"
            surface.contains("grass") || description.contains("césped") ||
                    description.contains("natural") -> "Natural"
            surface.contains("concrete") || surface.contains("asphalt") ||
                    description.contains("cemento") || description.contains("concreto") -> "Cemento"
            else -> "Sintética"
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}
