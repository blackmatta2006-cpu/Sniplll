package com.example.util

import com.example.data.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineUtil {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun distance(a: GeoPoint, b: GeoPoint): Double {
        val radLatA = Math.toRadians(a.lat)
        val radLatB = Math.toRadians(b.lat)
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lng - a.lng)

        val x = sin(dLat / 2) * sin(dLat / 2) +
                cos(radLatA) * cos(radLatB) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(x), sqrt(1 - x))
        return EARTH_RADIUS_METERS * c
    }

    fun calculateTotalLength(points: List<GeoPoint>): Long {
        if (points.size < 2) return 0L
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += distance(points[i], points[i + 1])
        }
        return Math.round(total)
    }

    /**
     * Finds the exact GPS point along a cable polyline at a specific distance from the start.
     */
    fun findPointAtDistance(points: List<GeoPoint>, targetMeters: Double): GeoPoint {
        if (points.isEmpty()) return GeoPoint(12.9716, 77.5946)
        if (points.size == 1 || targetMeters <= 0.0) return points.first()

        var accumulated = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val segDist = distance(p1, p2)

            if (accumulated + segDist >= targetMeters) {
                val remaining = targetMeters - accumulated
                val fraction = if (segDist > 0) (remaining / segDist).coerceIn(0.0, 1.0) else 0.0
                val lat = p1.lat + (p2.lat - p1.lat) * fraction
                val lng = p1.lng + (p2.lng - p1.lng) * fraction
                return GeoPoint(lat, lng)
            }
            accumulated += segDist
        }
        return points.last()
    }

    fun getGoogleMapsUrl(lat: Double, lng: Double): String {
        return "https://www.google.com/maps?q=${String.format(java.util.Locale.US, "%.6f,%.6f", lat, lng)}"
    }

    fun getStreetViewUrl(lat: Double, lng: Double): String {
        return "https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${String.format(java.util.Locale.US, "%.6f,%.6f", lat, lng)}"
    }
}
