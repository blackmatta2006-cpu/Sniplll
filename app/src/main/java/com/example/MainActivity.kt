package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdminDrawDialog
import com.example.ui.components.AppHeaderWithSearch
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CutAssistantScreen
import com.example.ui.screens.CutHistoryScreen
import com.example.ui.screens.FullPageMapScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RoutesListScreen
import com.example.ui.theme.CutRed
import com.example.ui.theme.OrangeAmber
import com.example.ui.theme.OrangeDark
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.ShreyNetworkTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CableViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShreyNetworkTheme {
                ShreyNetworkApp()
            }
        }
    }
}

@Composable
fun ShreyNetworkApp(
    viewModel: CableViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("shrey_network_prefs", Context.MODE_PRIVATE) }

    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val filteredRoutes by viewModel.filteredRoutes.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val cutReports by viewModel.cutReports.collectAsStateWithLifecycle()
    val adminPhones by viewModel.adminPhones.collectAsStateWithLifecycle()
    val isDrawing by viewModel.isDrawing.collectAsStateWithLifecycle()
    val drawPath by viewModel.drawPath.collectAsStateWithLifecycle()
    val drawJointBoxes by viewModel.drawJointBoxes.collectAsStateWithLifecycle()
    val drawnLengthMeters by viewModel.drawnLengthMeters.collectAsStateWithLifecycle()
    val isSnapToRoad by viewModel.isSnapToRoadEnabled.collectAsStateWithLifecycle()
    val isAutoDrive by viewModel.isAutoDriveActive.collectAsStateWithLifecycle()
    val driveSpeedKmh by viewModel.driveSpeedKmh.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDrawNameDialog by remember { mutableStateOf(false) }

    // Request Location permission for driving auto-survey
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle result
    }

    LaunchedEffect(isAutoDrive) {
        if (isAutoDrive) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // GPS location listener for driving auto-survey
    DisposableEffect(isAutoDrive) {
        if (!isAutoDrive) return@DisposableEffect onDispose {}

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .setMinUpdateDistanceMeters(10f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val speedKmh = (loc.speed * 3.6f) // m/s to km/h
                viewModel.onGpsLocationUpdate(loc.latitude, loc.longitude, speedKmh)
            }
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {}

        onDispose {
            try {
                fusedClient.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Handle Back Press when not on primary Routes tab
    if (userSession != null && currentTab != AppTab.ROUTES) {
        BackHandler {
            if (isDrawing) {
                viewModel.cancelDrawing()
            }
            viewModel.setTab(AppTab.ROUTES)
        }
    }

    if (userSession == null) {
        LoginScreen(
            initialPhone = "", // Empty field for manual entry
            onLoginSuccess = { phone ->
                sharedPrefs.edit().putString("saved_phone", phone).apply()
                viewModel.login(phone)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AppHeaderWithSearch(
                    userSession = userSession,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    activeFilter = activeFilter,
                    onFilterChange = { viewModel.setFilter(it) },
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { viewModel.setSearchExpanded(!isSearchExpanded) },
                    isLoading = isLoading,
                    onRefresh = { viewModel.refresh() },
                    onLogout = {
                        sharedPrefs.edit().remove("saved_phone").apply()
                        viewModel.logout()
                    },
                    showFilters = currentTab == AppTab.ROUTES
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_nav_bar")
                ) {
                    // Routes Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.ROUTES,
                        onClick = { viewModel.setTab(AppTab.ROUTES) },
                        icon = {
                            Icon(imageVector = Icons.Default.Route, contentDescription = "Routes")
                        },
                        label = { Text("Routes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangeDark,
                            selectedTextColor = OrangeDark,
                            indicatorColor = OrangePrimary.copy(alpha = 0.2f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_routes")
                    )

                    // Full Map Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.FULL_MAP,
                        onClick = { viewModel.setTab(AppTab.FULL_MAP) },
                        icon = {
                            Icon(imageVector = Icons.Default.Map, contentDescription = "Full Map")
                        },
                        label = { Text("Full Map", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.2f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_full_map")
                    )

                    // Cut Assistant Chat Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.CUT_ASSISTANT,
                        onClick = { viewModel.setTab(AppTab.CUT_ASSISTANT) },
                        icon = {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "Cut Report")
                        },
                        label = { Text("Cut Report", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangeDark,
                            selectedTextColor = OrangeDark,
                            indicatorColor = OrangePrimary.copy(alpha = 0.2f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_cut_assistant")
                    )

                    // Incidents Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.CUT_HISTORY,
                        onClick = { viewModel.setTab(AppTab.CUT_HISTORY) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (cutReports.isNotEmpty()) {
                                        Badge(containerColor = CutRed) {
                                            Text(
                                                cutReports.size.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = "Incidents")
                            }
                        },
                        label = { Text("Incidents", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CutRed,
                            selectedTextColor = CutRed,
                            indicatorColor = CutRed.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_cut_history")
                    )

                    // Admin Panel Tab (Visible to Admins)
                    if (userSession?.isAdmin == true) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.ADMIN,
                            onClick = { viewModel.setTab(AppTab.ADMIN) },
                            icon = {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Admins")
                            },
                            label = { Text("Admins", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangeDark,
                                selectedTextColor = OrangeDark,
                                indicatorColor = OrangeAmber.copy(alpha = 0.25f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("tab_admin")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.ROUTES -> {
                            RoutesListScreen(
                                routes = filteredRoutes,
                                selectedRoute = selectedRoute,
                                cutReports = cutReports,
                                userSession = userSession,
                                searchQuery = searchQuery,
                                onRouteSelected = { route, openMap ->
                                    viewModel.selectRoute(route, navigateToMap = openMap)
                                },
                                onDeleteRoute = { route ->
                                    viewModel.deleteRoute(route)
                                },
                                onStartDrawRoute = { showDrawNameDialog = true },
                                onReportCutForRoute = { route ->
                                    viewModel.selectRoute(route)
                                    viewModel.setTab(AppTab.CUT_ASSISTANT)
                                },
                                onClearSearch = { viewModel.setSearchQuery("") }
                            )
                        }

                        AppTab.FULL_MAP -> {
                            FullPageMapScreen(
                                selectedRoute = selectedRoute,
                                cutReports = cutReports,
                                userSession = userSession,
                                isDrawingMode = isDrawing,
                                drawPath = drawPath,
                                drawJointBoxes = drawJointBoxes,
                                drawnLengthMeters = drawnLengthMeters,
                                isSnapToRoad = isSnapToRoad,
                                isAutoDrive = isAutoDrive,
                                driveSpeedKmh = driveSpeedKmh,
                                onAddDrawPoint = { pt -> viewModel.addPointToDraw(pt) },
                                onMoveDirection = { dir -> viewModel.moveInDirection(dir) },
                                onAddLoopJointBox = { loopMeters -> viewModel.addLoopJointBox(loopMeters) },
                                onToggleSnapToRoad = { viewModel.toggleSnapToRoad() },
                                onToggleAutoDrive = { viewModel.toggleAutoDrive() },
                                onUndoDrawPoint = { viewModel.undoLastDrawPoint() },
                                onSaveDrawnRoute = { viewModel.saveDrawnRoute() },
                                onCancelDrawing = { viewModel.cancelDrawing() },
                                onReportCutRequested = { lat, lng ->
                                    viewModel.processCutReportRequest()
                                    viewModel.setTab(AppTab.CUT_ASSISTANT)
                                }
                            )
                        }

                        AppTab.CUT_ASSISTANT -> {
                            CutAssistantScreen(
                                selectedRoute = selectedRoute,
                                chatMessages = chatMessages,
                                onSendMessage = { text -> viewModel.sendChatMessage(text) },
                                onReportCutAtDistance = { dist -> viewModel.reportCutAtDistance(dist) },
                                onSelectRouteClick = { viewModel.setTab(AppTab.ROUTES) }
                            )
                        }

                        AppTab.CUT_HISTORY -> {
                            CutHistoryScreen(
                                cutReports = cutReports,
                                onViewOnMapClick = { report ->
                                    val r = filteredRoutes.find { it.id == report.routeId }
                                    if (r != null) viewModel.selectRoute(r)
                                    viewModel.setTab(AppTab.FULL_MAP)
                                }
                            )
                        }

                        AppTab.ADMIN -> {
                            AdminPanelScreen(
                                adminPhones = adminPhones,
                                onCreateAdmin = { phone -> viewModel.createNewAdmin(phone) }
                            )
                        }
                    }
                }

                // Admin Dialog to Start Drawing
                AdminDrawDialog(
                    isOpen = showDrawNameDialog,
                    onDismiss = { showDrawNameDialog = false },
                    onConfirm = { name ->
                        showDrawNameDialog = false
                        viewModel.startDrawing(name)
                    }
                )
            }
        }
    }
}
