package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PlaceSearchResult(
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lng: Double
)

object PlaceSearchUtil {
    private const val TAG = "PlaceSearch"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun searchPlace(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&countrycodes=in&limit=5"
            val request = Request.Builder()
                .url(url)
                // Strict compliance with OSM Nominatim usage policy
                .header("User-Agent", "ShreyNetwork/1.0 (Android; blackmatta2006@gmail.com)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string().orEmpty()
            val array = JSONArray(body)
            val results = mutableListOf<PlaceSearchResult>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val fullDisplayName = obj.optString("display_name", "")
                val lat = obj.optDouble("lat", 0.0)
                val lon = obj.optDouble("lon", 0.0)
                val parts = fullDisplayName.split(",")
                val short = parts.firstOrNull()?.trim() ?: fullDisplayName

                if (lat != 0.0 && lon != 0.0) {
                    results.add(
                        PlaceSearchResult(
                            displayName = fullDisplayName,
                            shortName = short,
                            lat = lat,
                            lng = lon
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search place error", e)
            emptyList()
        }
    }
}
