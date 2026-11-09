package com.example.data.repository

import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.GeoPoint
import com.example.data.remote.FirestoreRemoteDataSource
import com.example.util.HaversineUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CableRepository(
    private val remoteDataSource: FirestoreRemoteDataSource = FirestoreRemoteDataSource()
) {
    private val _routes = MutableStateFlow<List<CableRoute>>(emptyList())
    val routes: StateFlow<List<CableRoute>> = _routes.asStateFlow()

    private val _cutReports = MutableStateFlow<List<CutReport>>(emptyList())
    val cutReports: StateFlow<List<CutReport>> = _cutReports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun refreshData(): Boolean {
        _isLoading.value = true
        _errorMessage.value = null

        val routesResult = remoteDataSource.fetchRoutes()
        val reportsResult = remoteDataSource.fetchCutReports()

        var success = false

        routesResult.onSuccess { fetchedRoutes ->
            if (fetchedRoutes.isNotEmpty()) {
                _routes.value = fetchedRoutes
                success = true
            } else if (_routes.value.isEmpty()) {
                // Seed fallback route if empty
                _routes.value = getDefaultSampleRoutes()
            }
        }.onFailure { err ->
            _errorMessage.value = "Firestore sync error: ${err.localizedMessage}"
            if (_routes.value.isEmpty()) {
                _routes.value = getDefaultSampleRoutes()
            }
        }

        reportsResult.onSuccess { fetchedReports ->
            _cutReports.value = fetchedReports
        }.onFailure {
            // Keep existing reports
        }

        _isLoading.value = false
        return success
    }

    suspend fun addRoute(name: String, coordinates: List<GeoPoint>, phone: String): CableRoute {
        val length = HaversineUtil.calculateTotalLength(coordinates)
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val tempId = UUID.randomUUID().toString().take(12)

        val newRoute = CableRoute(
            id = tempId,
            name = name,
            coordinates = coordinates,
            lengthMeters = length,
            createdBy = phone,
            createdAt = nowIso
        )

        // Add to local state immediately
        _routes.value = listOf(newRoute) + _routes.value

        // Try pushing to Firestore
        val remoteResult = remoteDataSource.saveRoute(newRoute)
        remoteResult.onSuccess { remoteId ->
            // Update with confirmed remote ID
            _routes.value = _routes.value.map {
                if (it.id == tempId) it.copy(id = remoteId) else it
            }
        }

        return newRoute
    }

    suspend fun addCutReport(
        route: CableRoute,
        lat: Double,
        lng: Double,
        phone: String,
        notes: String = "Cable cut reported"
    ): CutReport {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val tempId = UUID.randomUUID().toString().take(12)
        val mapsUrl = HaversineUtil.getGoogleMapsUrl(lat, lng)
        val svUrl = HaversineUtil.getStreetViewUrl(lat, lng)

        val newReport = CutReport(
            id = tempId,
            routeId = route.id,
            routeName = route.name,
            lat = lat,
            lng = lng,
            reportedBy = phone,
            timestamp = nowIso,
            mapsLink = mapsUrl,
            streetViewLink = svUrl,
            notes = notes,
            status = "Open"
        )

        _cutReports.value = listOf(newReport) + _cutReports.value

        remoteDataSource.saveCutReport(newReport).onSuccess { remoteId ->
            _cutReports.value = _cutReports.value.map {
                if (it.id == tempId) it.copy(id = remoteId) else it
            }
        }

        return newReport
    }

    private fun getDefaultSampleRoutes(): List<CableRoute> {
        return listOf(
            CableRoute(
                id = "sample-bengaluru-central",
                name = "1 test",
                coordinates = listOf(
                    GeoPoint(13.00917, 77.58129),
                    GeoPoint(13.00972, 77.58126),
                    GeoPoint(13.01076, 77.58123),
                    GeoPoint(13.01083, 77.58091),
                    GeoPoint(13.01086, 77.58070),
                    GeoPoint(13.01120, 77.58046),
                    GeoPoint(13.01147, 77.57965),
                    GeoPoint(13.01031, 77.57959),
                    GeoPoint(13.01035, 77.57879),
                    GeoPoint(13.01154, 77.57800),
                    GeoPoint(13.00943, 77.57798)
                ),
                lengthMeters = 1096,
                createdBy = "6361788965",
                createdAt = "2026-11-09T06:59:08.490Z"
            )
        )
    }
}
