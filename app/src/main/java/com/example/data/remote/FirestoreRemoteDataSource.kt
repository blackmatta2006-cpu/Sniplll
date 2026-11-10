package com.example.data.remote

import android.util.Log
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.GeoPoint
import com.example.data.model.LoopJointBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirestoreRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "FirestoreRemote"
        private const val API_KEY = "AIzaSyCdV5Bnn6lZowj68NRfIulSxpo2c_-1qd4"
        private const val PROJECT_ID = "snipl-60579"
        private const val BASE_URL = "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"
    }

    suspend fun fetchRoutes(): Result<List<CableRoute>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/routes?key=$API_KEY"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyString = response.body?.string().orEmpty()
            val root = JSONObject(bodyString)
            val docsArray = root.optJSONArray("documents") ?: JSONArray()
            val routes = mutableListOf<CableRoute>()

            for (i in 0 until docsArray.length()) {
                val doc = docsArray.getJSONObject(i)
                val fullPath = doc.optString("name", "")
                val id = fullPath.substringAfterLast("/")
                val fields = doc.optJSONObject("fields") ?: JSONObject()

                val name = fields.optJSONObject("name")?.optString("stringValue", "Unnamed Route") ?: "Unnamed Route"
                val lengthMeters = fields.optJSONObject("lengthMeters")?.optLong("integerValue")
                    ?: fields.optJSONObject("lengthMeters")?.optDouble("doubleValue")?.toLong()
                    ?: 0L
                val createdBy = fields.optJSONObject("createdBy")?.optString("stringValue", "Unknown") ?: "Unknown"
                val createdAt = fields.optJSONObject("createdAt")?.optString("timestampValue", "")
                    ?: doc.optString("createTime", "")

                // Parse coordinates array
                val coordsList = mutableListOf<GeoPoint>()
                val coordsArray = fields.optJSONObject("coordinates")
                    ?.optJSONObject("arrayValue")
                    ?.optJSONArray("values")

                if (coordsArray != null) {
                    for (c in 0 until coordsArray.length()) {
                        val mapVal = coordsArray.getJSONObject(c).optJSONObject("mapValue")?.optJSONObject("fields")
                        if (mapVal != null) {
                            val lat = mapVal.optJSONObject("lat")?.optDouble("doubleValue") ?: 0.0
                            val lng = mapVal.optJSONObject("lng")?.optDouble("doubleValue") ?: 0.0
                            if (lat != 0.0 || lng != 0.0) {
                                coordsList.add(GeoPoint(lat, lng))
                            }
                        }
                    }
                }

                // Parse jointBoxes if present
                val jointBoxesList = mutableListOf<LoopJointBox>()
                val jbArray = fields.optJSONObject("jointBoxes")
                    ?.optJSONObject("arrayValue")
                    ?.optJSONArray("values")
                if (jbArray != null) {
                    for (j in 0 until jbArray.length()) {
                        val jMap = jbArray.getJSONObject(j).optJSONObject("mapValue")?.optJSONObject("fields")
                        if (jMap != null) {
                            val jId = jMap.optJSONObject("id")?.optString("stringValue", "jb_$j") ?: "jb_$j"
                            val jName = jMap.optJSONObject("name")?.optString("stringValue", "Joint Box") ?: "Joint Box"
                            val jLat = jMap.optJSONObject("lat")?.optDouble("doubleValue") ?: 0.0
                            val jLng = jMap.optJSONObject("lng")?.optDouble("doubleValue") ?: 0.0
                            val jLoop = jMap.optJSONObject("loopMeters")?.optInt("integerValue", 20) ?: 20
                            val jPhoto = jMap.optJSONObject("photoUrl")?.optString("stringValue", "") ?: ""
                            jointBoxesList.add(LoopJointBox(jId, jLat, jLng, jLoop, jName, jPhoto))
                        }
                    }
                }

                routes.add(
                    CableRoute(
                        id = id,
                        name = name,
                        coordinates = coordsList,
                        lengthMeters = lengthMeters,
                        createdBy = createdBy,
                        createdAt = createdAt,
                        jointBoxes = jointBoxesList
                    )
                )
            }

            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching routes", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRoute(routeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/routes/$routeId?key=$API_KEY"
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Delete failed with HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting route", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCutReports(): Result<List<CutReport>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/cutReports?key=$API_KEY"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bodyString = response.body?.string().orEmpty()
            val root = JSONObject(bodyString)
            val docsArray = root.optJSONArray("documents") ?: JSONArray()
            val reports = mutableListOf<CutReport>()

            for (i in 0 until docsArray.length()) {
                val doc = docsArray.getJSONObject(i)
                val fullPath = doc.optString("name", "")
                val id = fullPath.substringAfterLast("/")
                val fields = doc.optJSONObject("fields") ?: JSONObject()

                val routeId = fields.optJSONObject("routeId")?.optString("stringValue", "") ?: ""
                val routeName = fields.optJSONObject("routeName")?.optString("stringValue", "Route") ?: "Route"
                val lat = fields.optJSONObject("lat")?.optDouble("doubleValue") ?: 0.0
                val lng = fields.optJSONObject("lng")?.optDouble("doubleValue") ?: 0.0
                val reportedBy = fields.optJSONObject("reportedBy")?.optString("stringValue", "") ?: ""
                val timestamp = fields.optJSONObject("timestamp")?.optString("timestampValue", "")
                    ?: fields.optJSONObject("createdAt")?.optString("timestampValue", "")
                    ?: doc.optString("createTime", "")
                val mapsLink = fields.optJSONObject("mapsLink")?.optString("stringValue", "") ?: ""
                val streetViewLink = fields.optJSONObject("streetViewLink")?.optString("stringValue", "") ?: ""
                val notes = fields.optJSONObject("notes")?.optString("stringValue", "") ?: ""
                val status = fields.optJSONObject("status")?.optString("stringValue", "Open") ?: "Open"

                reports.add(
                    CutReport(
                        id = id,
                        routeId = routeId,
                        routeName = routeName,
                        lat = lat,
                        lng = lng,
                        reportedBy = reportedBy,
                        timestamp = timestamp,
                        mapsLink = mapsLink,
                        streetViewLink = streetViewLink,
                        notes = notes,
                        status = status
                    )
                )
            }

            Result.success(reports)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cut reports", e)
            Result.failure(e)
        }
    }

    suspend fun saveRoute(route: CableRoute): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/routes?key=$API_KEY"

            val coordsArray = JSONArray()
            for (p in route.coordinates) {
                val ptMap = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("lat", JSONObject().put("doubleValue", p.lat))
                        put("lng", JSONObject().put("doubleValue", p.lng))
                    })
                }
                coordsArray.put(JSONObject().put("mapValue", ptMap))
            }

            val jbArray = JSONArray()
            for (jb in route.jointBoxes) {
                val jMap = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("id", JSONObject().put("stringValue", jb.id))
                        put("name", JSONObject().put("stringValue", jb.name))
                        put("lat", JSONObject().put("doubleValue", jb.lat))
                        put("lng", JSONObject().put("doubleValue", jb.lng))
                        put("loopMeters", JSONObject().put("integerValue", jb.loopMeters.toString()))
                        if (jb.photoUrl.isNotEmpty()) {
                            put("photoUrl", JSONObject().put("stringValue", jb.photoUrl))
                        }
                    })
                }
                jbArray.put(JSONObject().put("mapValue", jMap))
            }

            val fields = JSONObject().apply {
                put("name", JSONObject().put("stringValue", route.name))
                put("lengthMeters", JSONObject().put("integerValue", route.lengthMeters.toString()))
                put("createdBy", JSONObject().put("stringValue", route.createdBy))
                put("coordinates", JSONObject().put("arrayValue", JSONObject().put("values", coordsArray)))
                put("jointBoxes", JSONObject().put("arrayValue", JSONObject().put("values", jbArray)))
            }

            val payload = JSONObject().apply {
                put("fields", fields)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respJson = JSONObject(response.body?.string().orEmpty())
                val newId = respJson.optString("name", "").substringAfterLast("/")
                Result.success(newId)
            } else {
                Result.failure(Exception("Firestore save failed with HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving route", e)
            Result.failure(e)
        }
    }

    suspend fun saveCutReport(report: CutReport): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/cutReports?key=$API_KEY"

            val fields = JSONObject().apply {
                put("routeId", JSONObject().put("stringValue", report.routeId))
                put("routeName", JSONObject().put("stringValue", report.routeName))
                put("lat", JSONObject().put("doubleValue", report.lat))
                put("lng", JSONObject().put("doubleValue", report.lng))
                put("reportedBy", JSONObject().put("stringValue", report.reportedBy))
                put("mapsLink", JSONObject().put("stringValue", report.mapsLink))
                put("streetViewLink", JSONObject().put("stringValue", report.streetViewLink))
                if (report.notes.isNotEmpty()) {
                    put("notes", JSONObject().put("stringValue", report.notes))
                }
            }

            val payload = JSONObject().apply {
                put("fields", fields)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respJson = JSONObject(response.body?.string().orEmpty())
                val newId = respJson.optString("name", "").substringAfterLast("/")
                Result.success(newId)
            } else {
                Result.failure(Exception("Firestore cutReport failed with HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cut report", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCutReport(reportId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/cutReports/$reportId?key=$API_KEY"
            val request = Request.Builder().url(url).delete().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Delete incident failed: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting incident", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAdmins(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/admins?key=$API_KEY"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            val adminPhones = mutableListOf("6361788965") // default primary admin
            if (response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val root = JSONObject(bodyString)
                val docsArray = root.optJSONArray("documents") ?: JSONArray()
                for (i in 0 until docsArray.length()) {
                    val doc = docsArray.getJSONObject(i)
                    val fields = doc.optJSONObject("fields") ?: JSONObject()
                    val phone = fields.optJSONObject("phone")?.optString("stringValue", "") ?: ""
                    if (phone.length == 10 && !adminPhones.contains(phone)) {
                        adminPhones.add(phone)
                    }
                }
            }
            Result.success(adminPhones)
        } catch (e: Exception) {
            Result.success(listOf("6361788965"))
        }
    }

    suspend fun saveAdmin(phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/admins?key=$API_KEY"
            val fields = JSONObject().apply {
                put("phone", JSONObject().put("stringValue", phone))
                put("createdAt", JSONObject().put("timestampValue", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())))
            }
            val payload = JSONObject().apply { put("fields", fields) }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
