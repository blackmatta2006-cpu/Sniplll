package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.DrawDirection
import com.example.data.model.GeoPoint
import com.example.data.model.LoopJointBox
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.repository.CableRepository
import com.example.util.HaversineUtil
import com.example.util.RoadRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val title: String) {
    ROUTES("Routes"),
    FULL_MAP("Full Map"),
    CUT_ASSISTANT("Cut Report"),
    CUT_HISTORY("Incidents"),
    ADMIN("Admins")
}

enum class RouteFilter(val label: String) {
    ALL("All Routes"),
    UNDER_1KM("< 1 km"),
    OVER_1KM("≥ 1 km"),
    HAS_CUTS("Reported Cuts")
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "bot" or "user"
    val text: String,
    val mapsUrl: String? = null,
    val streetViewUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class CableViewModel(
    private val repository: CableRepository = CableRepository()
) : ViewModel() {

    companion object {
        const val PRIMARY_ADMIN = "6361788965"
    }

    // Auth & Session
    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.ROUTES)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Header Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(RouteFilter.ALL)
    val activeFilter: StateFlow<RouteFilter> = _activeFilter.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    // Selected Route
    private val _selectedRoute = MutableStateFlow<CableRoute?>(null)
    val selectedRoute: StateFlow<CableRoute?> = _selectedRoute.asStateFlow()

    // Cut Reports & Admins
    val cutReports: StateFlow<List<CutReport>> = repository.cutReports
    val adminPhones: StateFlow<List<String>> = repository.adminPhones
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val errorMessage: StateFlow<String?> = repository.errorMessage

    // Filtered Routes
    val filteredRoutes: StateFlow<List<CableRoute>> = combine(
        repository.routes,
        _searchQuery,
        _activeFilter,
        cutReports
    ) { allRoutes, query, filter, reports ->
        val trimmed = query.trim().lowercase()
        val queryMatched = if (trimmed.isEmpty()) {
            allRoutes
        } else {
            allRoutes.filter {
                it.name.lowercase().contains(trimmed) ||
                it.id.lowercase().contains(trimmed) ||
                it.createdBy.lowercase().contains(trimmed)
            }
        }

        when (filter) {
            RouteFilter.ALL -> queryMatched
            RouteFilter.UNDER_1KM -> queryMatched.filter { it.totalCableLengthMeters < 1000 }
            RouteFilter.OVER_1KM -> queryMatched.filter { it.totalCableLengthMeters >= 1000 }
            RouteFilter.HAS_CUTS -> {
                val routeIdsWithCuts = reports.map { it.routeId }.toSet()
                queryMatched.filter { routeIdsWithCuts.contains(it.id) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Drawing State
    private val _isDrawing = MutableStateFlow(false)
    val isDrawing: StateFlow<Boolean> = _isDrawing.asStateFlow()

    private val _drawPath = MutableStateFlow<List<GeoPoint>>(emptyList())
    val drawPath: StateFlow<List<GeoPoint>> = _drawPath.asStateFlow()

    private val _drawJointBoxes = MutableStateFlow<List<LoopJointBox>>(emptyList())
    val drawJointBoxes: StateFlow<List<LoopJointBox>> = _drawJointBoxes.asStateFlow()

    private val _newRouteName = MutableStateFlow("")
    val newRouteName: StateFlow<String> = _newRouteName.asStateFlow()

    private val _isSnapToRoadEnabled = MutableStateFlow(true)
    val isSnapToRoadEnabled: StateFlow<Boolean> = _isSnapToRoadEnabled.asStateFlow()

    private val _isAutoDriveActive = MutableStateFlow(false)
    val isAutoDriveActive: StateFlow<Boolean> = _isAutoDriveActive.asStateFlow()

    private val _driveSpeedKmh = MutableStateFlow(0f)
    val driveSpeedKmh: StateFlow<Float> = _driveSpeedKmh.asStateFlow()

    private val _currentHeading = MutableStateFlow(0.0)
    val currentHeading: StateFlow<Double> = _currentHeading.asStateFlow()

    private val _currentGpsLocation = MutableStateFlow<GeoPoint?>(null)
    val currentGpsLocation: StateFlow<GeoPoint?> = _currentGpsLocation.asStateFlow()

    private val _mapRotationDegrees = MutableStateFlow(0f)
    val mapRotationDegrees: StateFlow<Float> = _mapRotationDegrees.asStateFlow()

    val drawnLengthMeters: StateFlow<Long> = combine(_drawPath, _drawJointBoxes) { path, jbs ->
        val polylineLen = HaversineUtil.calculateTotalLength(path)
        val loopExtra = jbs.sumOf { it.loopMeters }
        polylineLen + loopExtra
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Chat Assistant State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "bot",
                text = "Namaskara! Shrey Network ಗೆ ಸ್ವಾಗತ. ಕೇಬಲ್ ರೂಟ್ ಆಯ್ಕೆ ಮಾಡಿ ಅಥವಾ 'estakke cut agide cable' ಎಂದು ಕೇಳಿ."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Status snackbar / toast notifications
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refresh()
    }

    fun login(phoneInput: String): Boolean {
        val clean = phoneInput.trim()
        if (!clean.matches(Regex("^\\d{10}$"))) {
            _statusMessage.value = "10 digit mobile number enter madi"
            return false
        }
        val isAdminUser = clean == PRIMARY_ADMIN || adminPhones.value.contains(clean)
        val role = if (isAdminUser) UserRole.ADMIN else UserRole.TECHNICIAN
        _userSession.value = UserSession(phone = clean, role = role)
        refresh()
        return true
    }

    fun logout() {
        _userSession.value = null
        _selectedRoute.value = null
        _currentTab.value = AppTab.ROUTES
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: RouteFilter) {
        _activeFilter.value = filter
    }

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
        if (!expanded) {
            _searchQuery.value = ""
        }
    }

    fun toggleSnapToRoad() {
        _isSnapToRoadEnabled.value = !_isSnapToRoadEnabled.value
    }

    fun toggleAutoDrive() {
        _isAutoDriveActive.value = !_isAutoDriveActive.value
        if (_isAutoDriveActive.value) {
            _statusMessage.value = "Auto-Drive ON: Driving live cable tracking started"
        } else {
            _statusMessage.value = "Auto-Drive Paused"
        }
    }

    fun selectRoute(route: CableRoute, navigateToMap: Boolean = false) {
        _selectedRoute.value = route
        addBotMessage("Route '${route.name}' (${route.formattedLength}) select aagide. Full Map nodi athava Cut Report tab ge hogi.")
        if (navigateToMap) {
            _currentTab.value = AppTab.FULL_MAP
        }
    }

    fun deleteRoute(route: CableRoute) {
        viewModelScope.launch {
            val success = repository.deleteRoute(route.id)
            if (success) {
                if (_selectedRoute.value?.id == route.id) {
                    _selectedRoute.value = null
                }
                _statusMessage.value = "Route '${route.name}' deleted successfully"
            } else {
                _statusMessage.value = "Route delete failed"
            }
        }
    }

    fun deleteCutReport(report: CutReport) {
        viewModelScope.launch {
            val res = repository.deleteCutReport(report.id)
            if (res) {
                _statusMessage.value = "Incident on '${report.routeName}' deleted successfully"
            } else {
                _statusMessage.value = "Failed to delete incident"
            }
        }
    }

    fun rotateMap(delta: Float) {
        val next = (_mapRotationDegrees.value + delta + 360f) % 360f
        _mapRotationDegrees.value = next
    }

    fun resetMapRotation() {
        _mapRotationDegrees.value = 0f
    }

    fun createNewAdmin(phone: String) {
        val clean = phone.trim()
        if (clean.length != 10 || !clean.all { it.isDigit() }) {
            _statusMessage.value = "10 digit mobile number enter madi"
            return
        }
        viewModelScope.launch {
            val res = repository.addAdmin(clean)
            if (res) {
                _statusMessage.value = "New Admin $clean added successfully!"
            } else {
                _statusMessage.value = "Admin already exists or failed to save"
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshData()
            if (_selectedRoute.value == null && repository.routes.value.isNotEmpty()) {
                _selectedRoute.value = repository.routes.value.first()
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Drawing Actions
    fun startDrawing(name: String) {
        if (name.isBlank()) {
            _statusMessage.value = "Route name enter madi"
            return
        }
        _newRouteName.value = name
        _drawPath.value = emptyList()
        _drawJointBoxes.value = emptyList()
        _isDrawing.value = true
        _isAutoDriveActive.value = false
        _currentTab.value = AppTab.FULL_MAP
        _statusMessage.value = "Step 1: ರಸ್ತೆಯ ಮೇಲೆ Starting Point ಸೆಲೆಕ್ಟ್ ಮಾಡಿ"
    }

    fun addPointToDraw(point: GeoPoint) {
        if (!_isDrawing.value) return

        val currentPath = _drawPath.value
        if (currentPath.isEmpty()) {
            // First starting point
            _drawPath.value = listOf(point)
            _statusMessage.value = "Starting point set! Direction (⬆️ ➡️ ⬅️ ⬇️) ಅಥವಾ ಮ್ಯಾಪ್ ಮೇಲೆ ಟ್ಯಾಪ್ ಮಾಡಿ."
            return
        }

        val lastPt = currentPath.last()
        _currentHeading.value = RoadRouter.calculateBearing(lastPt, point)

        if (_isSnapToRoadEnabled.value) {
            viewModelScope.launch {
                val roadSnapped = RoadRouter.getRoadPath(lastPt, point)
                // Filter out first if it matches last
                val toAppend = if (roadSnapped.size > 1) roadSnapped.drop(1) else listOf(point)
                _drawPath.value = _drawPath.value + toAppend
            }
        } else {
            _drawPath.value = _drawPath.value + point
        }
    }

    fun moveInDirection(direction: DrawDirection) {
        if (!_isDrawing.value) return
        val currentPath = _drawPath.value
        if (currentPath.isEmpty()) {
            _statusMessage.value = "ಮೊದಲು ಮ್ಯಾಪ್ ಮೇಲೆ Starting Point ಟ್ಯಾಪ್ ಮಾಡಿ!"
            return
        }

        val lastPoint = currentPath.last()
        val baseBearing = if (currentPath.size >= 2) {
            RoadRouter.calculateBearing(currentPath[currentPath.size - 2], lastPoint)
        } else {
            _currentHeading.value
        }

        val targetBearing = (baseBearing + direction.bearingDelta + 360.0) % 360.0
        _currentHeading.value = targetBearing

        // Project 50m in that direction along road
        val projected = RoadRouter.projectPoint(lastPoint, 50.0, targetBearing)

        viewModelScope.launch {
            if (_isSnapToRoadEnabled.value) {
                val roadSegment = RoadRouter.getRoadPath(lastPoint, projected)
                val toAppend = if (roadSegment.size > 1) roadSegment.drop(1) else listOf(projected)
                _drawPath.value = _drawPath.value + toAppend
            } else {
                _drawPath.value = _drawPath.value + projected
            }
        }
    }

    fun addLoopJointBox(loopMeters: Int = 20, photoUrl: String = "", customName: String = "") {
        if (!_isDrawing.value) return
        val currentPath = _drawPath.value
        if (currentPath.isEmpty()) {
            _statusMessage.value = "Starting point ಇಲ್ಲದೆ Joint Box ಸೇರಿಸಲು ಸಾಧ್ಯವಿಲ್ಲ"
            return
        }

        val lastPt = currentPath.last()
        val boxNum = _drawJointBoxes.value.size + 1
        val finalName = if (customName.isNotBlank()) customName.trim() else "JC Box $boxNum (${loopMeters}m Loop)"
        val newBox = LoopJointBox(
            id = "jb_${UUID.randomUUID().toString().take(6)}",
            lat = lastPt.lat,
            lng = lastPt.lng,
            loopMeters = loopMeters,
            name = finalName,
            photoUrl = photoUrl.trim()
        )
        _drawJointBoxes.value = _drawJointBoxes.value + newBox
        val photoInfo = if (photoUrl.isNotBlank()) " with Photo" else ""
        _statusMessage.value = "Loop Joint Box ($loopMeters m Reserve)$photoInfo Added!"
    }

    fun onGpsLocationUpdate(lat: Double, lng: Double, speedKmh: Float) {
        _currentGpsLocation.value = GeoPoint(lat, lng)
        _driveSpeedKmh.value = speedKmh
        if (!_isDrawing.value || !_isAutoDriveActive.value) return

        val newPoint = GeoPoint(lat, lng)
        val currentPath = _drawPath.value
        if (currentPath.isEmpty()) {
            _drawPath.value = listOf(newPoint)
            return
        }

        val lastPoint = currentPath.last()
        val dist = HaversineUtil.distance(lastPoint, newPoint)
        // Add point if vehicle moved at least 15 meters
        if (dist >= 15.0) {
            viewModelScope.launch {
                val roadSegment = if (_isSnapToRoadEnabled.value) {
                    RoadRouter.getRoadPath(lastPoint, newPoint)
                } else {
                    listOf(lastPoint, newPoint)
                }
                val toAppend = if (roadSegment.size > 1) roadSegment.drop(1) else listOf(newPoint)
                _drawPath.value = _drawPath.value + toAppend
            }
        }
    }

    fun undoLastDrawPoint() {
        if (_drawPath.value.size > 1) {
            _drawPath.value = _drawPath.value.dropLast(1)
        } else if (_drawPath.value.size == 1) {
            _drawPath.value = emptyList()
            _statusMessage.value = "Starting point cleared"
        }
    }

    fun cancelDrawing() {
        _isDrawing.value = false
        _isAutoDriveActive.value = false
        _drawPath.value = emptyList()
        _drawJointBoxes.value = emptyList()
        _newRouteName.value = ""
    }

    fun saveDrawnRoute() {
        val name = _newRouteName.value.trim()
        val path = _drawPath.value
        val jointBoxes = _drawJointBoxes.value
        val phone = _userSession.value?.phone ?: PRIMARY_ADMIN

        if (name.isBlank()) {
            _statusMessage.value = "Route name required"
            return
        }
        if (path.size < 2) {
            _statusMessage.value = "At least 2 road points required"
            return
        }

        viewModelScope.launch {
            val saved = repository.addRoute(name, path, phone, jointBoxes)
            _selectedRoute.value = saved
            _isDrawing.value = false
            _isAutoDriveActive.value = false
            _drawPath.value = emptyList()
            _drawJointBoxes.value = emptyList()
            _newRouteName.value = ""
            _statusMessage.value = "Route '$name' saved with ${jointBoxes.size} Joint Boxes!"
            _currentTab.value = AppTab.ROUTES
        }
    }

    // Cut Report actions
    fun sendChatMessage(input: String) {
        val msg = input.trim()
        if (msg.isEmpty()) return

        addUserMessage(msg)

        val lower = msg.lowercase()
        if (lower.contains("cut") || lower.contains("estakke") || lower.contains("cable") || lower.contains("report")) {
            processCutReportRequest()
        } else if (lower.contains("hello") || lower.contains("hi") || lower.contains("namaskara")) {
            addBotMessage("Namaskara! Route select madi, nantara 'estakke cut agide cable' anta type madi.")
        } else {
            addBotMessage("Route select madi, then 'estakke cut agide cable' type madi athava quick button press madi.")
        }
    }

    fun reportCutAtDistance(meters: Double) {
        val current = _selectedRoute.value
        if (current == null) {
            addBotMessage("First Routes tab alli route click madi.")
            return
        }
        if (current.coordinates.isEmpty()) {
            addBotMessage("Route coordinates data illa.")
            return
        }

        val cutPoint = HaversineUtil.findPointAtDistance(current.coordinates, meters)
        recordAndAnnounceCut(current, cutPoint.lat, cutPoint.lng, "Cut reported at ${meters.toLong()}m from start")
    }

    fun processCutReportRequest() {
        val current = _selectedRoute.value
        if (current == null) {
            addBotMessage("First Routes tab alli route click madi.")
            return
        }
        if (current.coordinates.isEmpty()) {
            addBotMessage("Route coordinates data illa.")
            return
        }

        val midIndex = current.coordinates.size / 2
        val mid = current.coordinates[midIndex]
        recordAndAnnounceCut(current, mid.lat, mid.lng, "Cut reported near midpoint (${current.lengthMeters / 2}m)")
    }

    private fun recordAndAnnounceCut(route: CableRoute, lat: Double, lng: Double, notes: String) {
        val phone = _userSession.value?.phone ?: PRIMARY_ADMIN
        val mapsUrl = HaversineUtil.getGoogleMapsUrl(lat, lng)
        val svUrl = HaversineUtil.getStreetViewUrl(lat, lng)

        viewModelScope.launch {
            repository.addCutReport(route, lat, lng, phone, notes)
        }

        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = "bot",
            text = "Cable Cut Location Identified for '${route.name}'!\n📍 Lat: ${String.format(java.util.Locale.US, "%.6f", lat)}, Lng: ${String.format(java.util.Locale.US, "%.6f", lng)}\n$notes",
            mapsUrl = mapsUrl,
            streetViewUrl = svUrl,
            lat = lat,
            lng = lng
        )
        _statusMessage.value = "Cut reported & GPS navigation links generated!"
    }

    private fun addUserMessage(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "user", text = text)
    }

    private fun addBotMessage(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(sender = "bot", text = text)
    }
}
