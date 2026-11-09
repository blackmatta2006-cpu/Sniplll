package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.GeoPoint
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.repository.CableRepository
import com.example.util.HaversineUtil
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
    CUT_HISTORY("Cut History"),
    ADMIN("Admin Survey")
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
        const val ADMIN_PHONE = "6361788965"
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

    // Selected Route for Inspection & Cut Reporting
    private val _selectedRoute = MutableStateFlow<CableRoute?>(null)
    val selectedRoute: StateFlow<CableRoute?> = _selectedRoute.asStateFlow()

    // Cut Reports
    val cutReports: StateFlow<List<CutReport>> = repository.cutReports
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
            RouteFilter.UNDER_1KM -> queryMatched.filter { it.lengthMeters < 1000 }
            RouteFilter.OVER_1KM -> queryMatched.filter { it.lengthMeters >= 1000 }
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

    private val _newRouteName = MutableStateFlow("")
    val newRouteName: StateFlow<String> = _newRouteName.asStateFlow()

    val drawnLengthMeters: StateFlow<Long> = _drawPath.combine(_isDrawing) { path, _ ->
        HaversineUtil.calculateTotalLength(path)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Chat Assistant State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "bot",
                text = "Namaskara! Route select madi. Cable cut calculation mattu Google Maps navigation ge ready idini."
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
            _statusMessage.value = "Dayavittu 10 digit mobile number enter madi"
            return false
        }
        val role = if (clean == ADMIN_PHONE) UserRole.ADMIN else UserRole.TECHNICIAN
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

    fun selectRoute(route: CableRoute, navigateToMap: Boolean = false) {
        _selectedRoute.value = route
        addBotMessage("Route '${route.name}' (${route.formattedLength}) select aagide. Full Map nodi athava Cut Report tab ge hogi.")
        if (navigateToMap) {
            _currentTab.value = AppTab.FULL_MAP
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshData()
            // Auto select first route if none selected
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
        _isDrawing.value = true
        _currentTab.value = AppTab.FULL_MAP
        _statusMessage.value = "Drawing mode ON. Map meledhe points tap madi."
    }

    fun addPointToDraw(point: GeoPoint) {
        if (!_isDrawing.value) return
        _drawPath.value = _drawPath.value + point
    }

    fun undoLastDrawPoint() {
        if (_drawPath.value.isNotEmpty()) {
            _drawPath.value = _drawPath.value.dropLast(1)
        }
    }

    fun cancelDrawing() {
        _isDrawing.value = false
        _drawPath.value = emptyList()
        _newRouteName.value = ""
    }

    fun saveDrawnRoute() {
        val name = _newRouteName.value.trim()
        val path = _drawPath.value
        val phone = _userSession.value?.phone ?: ADMIN_PHONE

        if (name.isBlank()) {
            _statusMessage.value = "Route name required"
            return
        }
        if (path.size < 2) {
            _statusMessage.value = "At least 2 points required"
            return
        }

        viewModelScope.launch {
            val saved = repository.addRoute(name, path, phone)
            _selectedRoute.value = saved
            _isDrawing.value = false
            _drawPath.value = emptyList()
            _newRouteName.value = ""
            _statusMessage.value = "Route '$name' saved successfully!"
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

        // Default to midpoint if no specific distance given
        val midIndex = current.coordinates.size / 2
        val mid = current.coordinates[midIndex]
        recordAndAnnounceCut(current, mid.lat, mid.lng, "Cut reported near midpoint (${current.lengthMeters / 2}m)")
    }

    private fun recordAndAnnounceCut(route: CableRoute, lat: Double, lng: Double, notes: String) {
        val phone = _userSession.value?.phone ?: "6361788965"
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
