package com.example

import com.example.data.model.CableRoute
import com.example.data.model.GeoPoint
import com.example.util.HaversineUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testHaversineDistanceCalculation() {
        // Distance between Bengaluru Vidhana Soudha & MG Road (~2.5km)
        val p1 = GeoPoint(12.9797, 77.5912)
        val p2 = GeoPoint(12.9756, 77.6066)
        val dist = HaversineUtil.distance(p1, p2)
        assertTrue("Distance should be approximately 1.7km", dist in 1600.0..1800.0)
    }

    @Test
    fun testTotalRouteLengthCalculation() {
        val points = listOf(
            GeoPoint(13.00917, 77.58129),
            GeoPoint(13.00972, 77.58126),
            GeoPoint(13.01076, 77.58123)
        )
        val length = HaversineUtil.calculateTotalLength(points)
        assertTrue("Polyline length should be positive", length > 0)
    }

    @Test
    fun testFindPointAlongCable() {
        val p1 = GeoPoint(13.0000, 77.5000)
        val p2 = GeoPoint(13.0100, 77.5000)
        val total = HaversineUtil.distance(p1, p2)

        val midpoint = HaversineUtil.findPointAtDistance(listOf(p1, p2), total / 2.0)
        assertEquals(13.0050, midpoint.lat, 0.0001)
        assertEquals(77.5000, midpoint.lng, 0.0001)
    }

    @Test
    fun testRouteLengthFormatting() {
        val routeShort = CableRoute("1", "Short", emptyList(), 450, "6361788965", "")
        assertEquals("450 m", routeShort.formattedLength)

        val routeLong = CableRoute("2", "Long", emptyList(), 2450, "6361788965", "")
        assertEquals("2.45 km", routeLong.formattedLength)
    }
}
