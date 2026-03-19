package dev.csse.pfnassar.daytripper.data

import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MapboxDirectionsRepository {
    private fun pointToLngLat(point: Point): Pair<Double, Double> {
        val json = JSONObject(point.toJson())
        val coordinates = json.getJSONArray("coordinates")
        return coordinates.getDouble(0) to coordinates.getDouble(1)
    }

    suspend fun fetchWalkingRoute(
        accessToken: String,
        origin: Point,
        destination: Point
    ): List<Point>? = withContext(Dispatchers.IO) {
        val (originLng, originLat) = pointToLngLat(origin)
        val (destinationLng, destinationLat) = pointToLngLat(destination)

        val endpoint =
            "https://api.mapbox.com/directions/v5/mapbox/walking/" +
                    "$originLng,$originLat;$destinationLng,$destinationLat" +
                    "?alternatives=false&geometries=geojson&overview=full&steps=true&access_token=$accessToken"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        try {
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseBody)
            val routes = responseJson.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null

            val geometry = routes.getJSONObject(0).optJSONObject("geometry") ?: return@withContext null
            val coordinates = geometry.optJSONArray("coordinates") ?: return@withContext null

            val points = mutableListOf<Point>()
            for (i in 0 until coordinates.length()) {
                val pair = coordinates.optJSONArray(i) ?: continue
                if (pair.length() < 2) continue
                points.add(
                    Point.fromLngLat(
                        pair.getDouble(0),
                        pair.getDouble(1)
                    )
                )
            }

            return@withContext points.takeIf { it.size >= 2 }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
