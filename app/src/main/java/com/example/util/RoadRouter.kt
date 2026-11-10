package com.example.util

import android.util.Log
import com.example.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RoadRouter {
    private const val TAG = "RoadRouter"
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    /**
     * Queries OSRM (Open Source Routing Machine) to get the exact road path between two points.
     * Snaps to actual road geometry with all curves and turns.
     */
    suspend fun getRoadPath(from: GeoPoint, to: GeoPoint): List<GeoPoint> = withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/${from.lng},${from.lat};${to.lng},${to.lat}?overview=full&geometries=geojson"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ShreyNetwork/1.0 (Android; blackmatta2006@gmail.com)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (json.optString("code") == "Ok") {
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).optJSONObject("geometry")
                        val coords = geometry?.optJSONArray("coordinates")
                        if (coords != null && coords.length() > 0) {
                            val roadPoints = mutableListOf<GeoPoint>()
                            for (i in 0 until coords.length()) {
                                val pt = coords.getJSONArray(i)
                                val lng = pt.getDouble(0)
                                val lat = pt.getDouble(1)
                                roadPoints.add(GeoPoint(lat, lng))
                            }
                            return@withContext roadPoints
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM road snapping failed, falling back to straight segment: ${e.message}")
        }
        // Fallback: direct segment
        listOf(from, to)
    }

    /**
     * Projects a coordinate distanceMeters away along a given bearing in degrees.
     */
    fun projectPoint(start: GeoPoint, distanceMeters: Double, bearingDegrees: Double): GeoPoint {
        val r = 6371000.0 // Earth radius in meters
        val d = distanceMeters / r
        val brng = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(start.lat)
        val lon1 = Math.toRadians(start.lng)

        val lat2 = Math.asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(brng))
        val lon2 = lon1 + Math.atan2(sin(brng) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))

        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /**
     * Calculates initial bearing between two points in degrees (0..360).
     */
    fun calculateBearing(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val dLon = Math.toRadians(to.lng - from.lng)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360.0) % 360.0
    }
}
