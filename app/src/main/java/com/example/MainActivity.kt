package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdminDrawDialog
import com.example.ui.components.AppHeaderWithSearch
import com.example.ui.screens.CutAssistantScreen
import com.example.ui.screens.CutHistoryScreen
import com.example.ui.screens.FullPageMapScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RoutesListScreen
import com.example.ui.theme.CableRoutesTheme
import com.example.ui.theme.CutRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FiberCyan
import com.example.ui.theme.TelecomNavy
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CableViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CableRoutesTheme {
                CableRoutesApp()
            }
        }
    }
}

@Composable
fun CableRoutesApp(
    viewModel: CableViewModel = viewModel()
) {
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val filteredRoutes by viewModel.filteredRoutes.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
    val cutReports by viewModel.cutReports.collectAsStateWithLifecycle()
    val isDrawing by viewModel.isDrawing.collectAsStateWithLifecycle()
    val drawPath by viewModel.drawPath.collectAsStateWithLifecycle()
    val drawnLengthMeters by viewModel.drawnLengthMeters.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDrawNameDialog by remember { mutableStateOf(false) }

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
            onLoginSuccess = { phone ->
                viewModel.login(phone)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                // Header with integrated search bar
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
                    onLogout = { viewModel.logout() },
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
                            selectedIconColor = TelecomNavy,
                            selectedTextColor = TelecomNavy,
                            indicatorColor = FiberCyan.copy(alpha = 0.35f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_routes")
                    )

                    // Full Map Tab ("Maps ella different pagealli full page irbeku")
                    NavigationBarItem(
                        selected = currentTab == AppTab.FULL_MAP,
                        onClick = { viewModel.setTab(AppTab.FULL_MAP) },
                        icon = {
                            Icon(imageVector = Icons.Default.Map, contentDescription = "Full Map")
                        },
                        label = { Text("Full Map", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            selectedTextColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.2f),
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
                            selectedIconColor = TelecomNavy,
                            selectedTextColor = TelecomNavy,
                            indicatorColor = FiberCyan.copy(alpha = 0.35f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("tab_cut_assistant")
                    )

                    // Cut History Incidents Tab
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
                                Icon(imageVector = Icons.Default.History, contentDescription = "Cut History")
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
                                onStartDrawRoute = { showDrawNameDialog = true },
                                onReportCutForRoute = { route ->
                                    viewModel.selectRoute(route)
                                    viewModel.setTab(AppTab.CUT_ASSISTANT)
                                },
                                onClearSearch = { viewModel.setSearchQuery("") }
                            )
                        }

                        AppTab.FULL_MAP -> {
                            // "Maps ella different pagealli full page irbeku"
                            FullPageMapScreen(
                                selectedRoute = selectedRoute,
                                cutReports = cutReports,
                                userSession = userSession,
                                isDrawingMode = isDrawing,
                                drawPath = drawPath,
                                drawnLengthMeters = drawnLengthMeters,
                                onAddDrawPoint = { pt -> viewModel.addPointToDraw(pt) },
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
                                    // Focus on route and map
                                    val r = filteredRoutes.find { it.id == report.routeId }
                                    if (r != null) viewModel.selectRoute(r)
                                    viewModel.setTab(AppTab.FULL_MAP)
                                }
                            )
                        }

                        AppTab.ADMIN -> {
                            // If direct survey tab activated
                            FullPageMapScreen(
                                selectedRoute = selectedRoute,
                                cutReports = cutReports,
                                userSession = userSession,
                                isDrawingMode = true,
                                drawPath = drawPath,
                                drawnLengthMeters = drawnLengthMeters,
                                onAddDrawPoint = { pt -> viewModel.addPointToDraw(pt) },
                                onUndoDrawPoint = { viewModel.undoLastDrawPoint() },
                                onSaveDrawnRoute = { viewModel.saveDrawnRoute() },
                                onCancelDrawing = { viewModel.cancelDrawing() },
                                onReportCutRequested = { _, _ -> }
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
