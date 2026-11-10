package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.DrawDirection
import com.example.data.model.GeoPoint
import com.example.data.model.LoopJointBox
import com.example.data.model.UserSession
import com.example.ui.theme.CutRed
import com.example.ui.theme.GoodGreen
import com.example.ui.theme.JointBoxPurple
import com.example.ui.theme.OrangeAmber
import com.example.ui.theme.OrangeDark
import com.example.ui.theme.OrangePrimary
import com.example.util.HaversineUtil
import com.example.util.PlaceSearchResult
import com.example.util.PlaceSearchUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MapWebInterface(
    private val onMapClick: (Double, Double) -> Unit,
    private val onMarkerClick: (String, Double, Double) -> Unit
) {
    @JavascriptInterface
    fun handleMapClick(lat: Double, lng: Double) {
        onMapClick(lat, lng)
    }

    @JavascriptInterface
    fun handleMarkerClick(title: String, lat: Double, lng: Double) {
        onMarkerClick(title, lat, lng)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullPageMapScreen(
    selectedRoute: CableRoute?,
    cutReports: List<CutReport>,
    userSession: UserSession?,
    isDrawingMode: Boolean,
    drawPath: List<GeoPoint>,
    drawJointBoxes: List<LoopJointBox>,
    drawnLengthMeters: Long,
    isSnapToRoad: Boolean,
    isAutoDrive: Boolean,
    driveSpeedKmh: Float,
    currentGpsLocation: GeoPoint?,
    mapRotationDegrees: Float,
    onAddDrawPoint: (GeoPoint) -> Unit,
    onMoveDirection: (DrawDirection) -> Unit,
    onAddLoopJointBox: (loopMeters: Int, photoUrl: String, name: String) -> Unit,
    onToggleSnapToRoad: () -> Unit,
    onToggleAutoDrive: () -> Unit,
    onRotateMap: (Float) -> Unit,
    onResetMapRotation: () -> Unit,
    onUndoDrawPoint: () -> Unit,
    onSaveDrawnRoute: () -> Unit,
    onCancelDrawing: () -> Unit,
    onReportCutRequested: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var selectedMarkerInfo by remember { mutableStateOf<String?>(null) }
    var showTileSwitcher by remember { mutableStateOf(false) }
    var showAddJointBoxDialog by remember { mutableStateOf(false) }
    var activeTileType by remember { mutableStateOf("osm") } // "osm", "osmfr", "satellite"

    // Place Search states
    var placeQuery by remember { mutableStateOf("") }
    var placeSearchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isSearchingPlaces by remember { mutableStateOf(false) }
    var searchDebounceJob by remember { mutableStateOf<Job?>(null) }

    val mapHtml = remember { getMapHtmlContent() }

    // Update Route, Cut Pins, Joint Boxes, and Drawing on Map
    LaunchedEffect(isMapLoaded, selectedRoute, cutReports, isDrawingMode, drawPath, drawJointBoxes, activeTileType) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect

        if (isDrawingMode) {
            val coordsJson = JSONArray().apply {
                drawPath.forEach { pt ->
                    put(JSONObject().apply {
                        put("lat", pt.lat)
                        put("lng", pt.lng)
                    })
                }
            }
            val jbJson = JSONArray().apply {
                drawJointBoxes.forEach { jb ->
                    put(JSONObject().apply {
                        put("lat", jb.lat)
                        put("lng", jb.lng)
                        put("name", jb.name)
                        put("loop", jb.loopMeters)
                        put("photoUrl", jb.photoUrl)
                    })
                }
            }
            wv.evaluateJavascript("setDrawingMode(true, $coordsJson, $jbJson);", null)
        } else {
            wv.evaluateJavascript("setDrawingMode(false, [], []);", null)

            // Render selected route
            if (selectedRoute != null && selectedRoute.coordinates.isNotEmpty()) {
                val coordsJson = JSONArray().apply {
                    selectedRoute.coordinates.forEach { pt ->
                        put(JSONObject().apply {
                            put("lat", pt.lat)
                            put("lng", pt.lng)
                        })
                    }
                }
                val jbJson = JSONArray().apply {
                    selectedRoute.jointBoxes.forEach { jb ->
                        put(JSONObject().apply {
                            put("lat", jb.lat)
                            put("lng", jb.lng)
                            put("name", jb.name)
                            put("loop", jb.loopMeters)
                            put("photoUrl", jb.photoUrl)
                        })
                    }
                }
                val nameEscaped = JSONObject.quote(selectedRoute.name)
                wv.evaluateJavascript(
                    "renderRoute($coordsJson, $nameEscaped, ${selectedRoute.lengthMeters}, $jbJson);",
                    null
                )
            } else {
                wv.evaluateJavascript("clearRoute();", null)
            }

            // Render cut markers
            val relevantCuts = if (selectedRoute != null) {
                cutReports.filter { it.routeId == selectedRoute.id }
            } else {
                cutReports
            }

            val cutsJson = JSONArray().apply {
                relevantCuts.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.routeName)
                        put("lat", c.lat)
                        put("lng", c.lng)
                        put("notes", c.notes)
                    })
                }
            }
            wv.evaluateJavascript("renderCuts($cutsJson);", null)
        }
    }

    // Update Current Location Marker on Map
    LaunchedEffect(isMapLoaded, currentGpsLocation) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect

        if (currentGpsLocation != null) {
            wv.evaluateJavascript(
                "updateCurrentLocation(${currentGpsLocation.lat}, ${currentGpsLocation.lng});",
                null
            )
        }
    }

    // Update Map Rotation in WebView
    LaunchedEffect(isMapLoaded, mapRotationDegrees) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isMapLoaded) return@LaunchedEffect
        wv.evaluateJavascript("setMapRotation($mapRotationDegrees);", null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Full Page Edge-to-Edge OpenStreetMap WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.databaseEnabled = true
                    // Official standard User-Agent complying with OpenStreetMap usage policy
                    settings.userAgentString = "ShreyNetwork/1.0 (Android; blackmatta2006@gmail.com)"
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }

                    addJavascriptInterface(
                        MapWebInterface(
                            onMapClick = { lat, lng ->
                                if (isDrawingMode) {
                                    onAddDrawPoint(GeoPoint(lat, lng))
                                }
                            },
                            onMarkerClick = { title, lat, lng ->
                                selectedMarkerInfo = "$title\n📍 ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)}"
                            }
                        ),
                        "AndroidBridge"
                    )

                    loadDataWithBaseURL("https://tile.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_page_map_webview")
        )

        // TOP AREA: PLACE SEARCH BAR (When not in drawing mode, or minimized)
        if (!isDrawingMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Floating Place Search Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Places",
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = placeQuery,
                            onValueChange = { newText ->
                                placeQuery = newText
                                searchDebounceJob?.cancel()
                                if (newText.length >= 2) {
                                    searchDebounceJob = coroutineScope.launch {
                                        delay(400)
                                        isSearchingPlaces = true
                                        val res = PlaceSearchUtil.searchPlace(newText)
                                        placeSearchResults = res
                                        isSearchingPlaces = false
                                    }
                                } else {
                                    placeSearchResults = emptyList()
                                }
                            },
                            placeholder = { Text("Search places / roads / areas...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = OrangePrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_place_search_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                            })
                        )

                        if (placeQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    placeQuery = ""
                                    placeSearchResults = emptyList()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Place Search Suggestions Dropdown
                if (placeSearchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                            items(placeSearchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            placeQuery = result.shortName
                                            placeSearchResults = emptyList()
                                            // Fly map to searched place & drop search marker
                                            val shortEscaped = JSONObject.quote(result.shortName)
                                            webViewRef?.evaluateJavascript(
                                                "flyToPlace(${result.lat}, ${result.lng}, $shortEscaped);",
                                                null
                                            )
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = result.shortName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2C2420))
                                        Text(
                                            text = result.displayName,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // DRAWING MODE HUD: Big Direction Controls & Survey Options
        if (isDrawingMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Info & Quick Actions Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E1E24),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(OrangePrimary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (drawPath.isEmpty()) "Step 1: Set Starting Point" else "Surveying Route",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = OrangePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "${drawPath.size} road points • ${drawnLengthMeters}m (${drawJointBoxes.size} Joint Boxes)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onUndoDrawPoint,
                                    enabled = drawPath.isNotEmpty(),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        .testTag("undo_draw_button")
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = onCancelDrawing,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CutRed.copy(alpha = 0.25f), CircleShape)
                                        .testTag("cancel_draw_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = CutRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Features Row: Road Snapping, Joint Box (+ Loop & Photo), Auto-Drive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Road Snapping Toggle
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSnapToRoad) OrangePrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleSnapToRoad() }
                                    .border(1.dp, if (isSnapToRoad) OrangePrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Route,
                                        contentDescription = null,
                                        tint = if (isSnapToRoad) OrangePrimary else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSnapToRoad) "Road Snap: ON" else "Road Snap: OFF",
                                        color = if (isSnapToRoad) OrangePrimary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Loop Joint Box Trigger with Photo URL Option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = JointBoxPurple.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAddJointBoxDialog = true }
                                    .border(1.dp, JointBoxPurple, RoundedCornerShape(10.dp))
                                    .testTag("add_joint_box_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFFB388FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Loop Joint Box (Photo)",
                                        color = Color(0xFFD1C4E9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Auto-Drive While Driving
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAutoDrive) GoodGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleAutoDrive() }
                                    .border(1.dp, if (isAutoDrive) GoodGreen else Color.Transparent, RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = if (isAutoDrive) GoodGreen else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAutoDrive) "Auto-Drive (${driveSpeedKmh.toInt()} km/h)" else "Auto-Drive",
                                        color = if (isAutoDrive) GoodGreen else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BIG DIRECTION CONTROLS ON MAP ("direction marks right left straight back dodda icons")
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    elevation = CardDefaults.elevatedCardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (drawPath.isEmpty()) "Tap map to set START point" else "Road Navigation Draw",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Directional Buttons Grid
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // STRAIGHT
                            Surface(
                                shape = CircleShape,
                                color = OrangePrimary,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable { onMoveDirection(DrawDirection.STRAIGHT) }
                                    .testTag("direction_straight")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Straight", tint = Color.White, modifier = Modifier.size(24.dp))
                                        Text("STRAIGHT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            // LEFT & RIGHT ROW
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(36.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // LEFT
                                Surface(
                                    shape = CircleShape,
                                    color = OrangePrimary,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                    .clickable { onMoveDirection(DrawDirection.LEFT) }
                                    .testTag("direction_left")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(24.dp))
                                            Text("LEFT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                // RIGHT
                                Surface(
                                    shape = CircleShape,
                                    color = OrangePrimary,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .clickable { onMoveDirection(DrawDirection.RIGHT) }
                                        .testTag("direction_right")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(24.dp))
                                            Text("RIGHT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // BACK
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6D4C41),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { onMoveDirection(DrawDirection.BACK) }
                                    .testTag("direction_back")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                                        Text("BACK", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MAP ROTATION & CONTROLS FLOATING BAR (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // COMPASS & NORTH RESET BUTTON ("map rotatable agbeku")
            FloatingActionButton(
                onClick = { onResetMapRotation() },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("compass_button"),
                containerColor = Color.White,
                contentColor = OrangePrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass / Reset North",
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(-mapRotationDegrees), // Needle points to real North
                        tint = if (mapRotationDegrees != 0f) OrangePrimary else Color(0xFF555555)
                    )
                }
            }

            // ROTATE LEFT BUTTON (-30°)
            FloatingActionButton(
                onClick = { onRotateMap(-30f) },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("rotate_left_button"),
                containerColor = Color.White,
                contentColor = OrangeDark,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateLeft,
                    contentDescription = "Rotate Left 30°",
                    modifier = Modifier.size(20.dp)
                )
            }

            // ROTATE RIGHT BUTTON (+30°)
            FloatingActionButton(
                onClick = { onRotateMap(30f) },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("rotate_right_button"),
                containerColor = Color.White,
                contentColor = OrangeDark,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate Right 30°",
                    modifier = Modifier.size(20.dp)
                )
            }

            // RECENTER ON GPS CURRENT LOCATION / ROUTE
            FloatingActionButton(
                onClick = {
                    if (currentGpsLocation != null) {
                        webViewRef?.evaluateJavascript(
                            "map.setView([${currentGpsLocation.lat}, ${currentGpsLocation.lng}], 16);",
                            null
                        )
                    } else {
                        webViewRef?.evaluateJavascript("fitBoundsToCurrent();", null)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("fit_bounds_button"),
                containerColor = Color.White,
                contentColor = if (currentGpsLocation != null) Color(0xFF2979FF) else OrangePrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(22.dp)
                )
            }

            // LAYER SWITCHER
            FloatingActionButton(
                onClick = { showTileSwitcher = !showTileSwitcher },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_layers_button"),
                containerColor = Color.White,
                contentColor = OrangeDark,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Map Layers",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Layer Switcher Dialog Popup
        if (showTileSwitcher) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 70.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1E24),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Map Tile Style",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "osm" to "OpenStreetMap (Standard)",
                        "osmfr" to "OpenStreetMap Clear",
                        "satellite" to "Satellite Imagery"
                    ).forEach { (type, label) ->
                        Button(
                            onClick = {
                                activeTileType = type
                                showTileSwitcher = false
                                webViewRef?.evaluateJavascript("switchTileLayer('$type');", null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTileType == type) OrangePrimary else Color.White.copy(alpha = 0.15f),
                                contentColor = if (activeTileType == type) Color.White else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(label, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // DIALOG: ADD LOOP JOINT BOX WITH PHOTO URL ("join box hakid takshana photo url hako option irbeku")
        if (showAddJointBoxDialog) {
            var selectedMeters by remember { mutableStateOf(20) }
            var jcName by remember { mutableStateOf("") }
            var photoUrlInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddJointBoxDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = JointBoxPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Loop Joint Box / ಜಾಯಿಂಟ್ ಬಾಕ್ಸ್",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OrangeDark
                            )
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Cable Slack Reserve (ಮೀಟರ್ ಲೂಪ್):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2420)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Loop selection pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15 to "15m", 20 to "20m", 30 to "30m", 50 to "50m").forEach { (meters, label) ->
                                Button(
                                    onClick = { selectedMeters = meters },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMeters == meters) OrangePrimary else Color(0xFFFFECE0),
                                        contentColor = if (selectedMeters == meters) Color.White else OrangeDark
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Joint Box Tag / ID
                        OutlinedTextField(
                            value = jcName,
                            onValueChange = { jcName = it },
                            label = { Text("Joint Box Name / ID") },
                            placeholder = { Text("e.g. JC-Chamber #4") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // PHOTO URL FIELD (User asked: "join box hakid takshana photo url hako option irbeku")
                        OutlinedTextField(
                            value = photoUrlInput,
                            onValueChange = { photoUrlInput = it },
                            label = { Text("Joint Box Photo URL (ಫೋಟೋ ಲಿಂಕ್)") },
                            placeholder = { Text("https://example.com/jc_photo.jpg") },
                            leadingIcon = {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = OrangePrimary)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "Enter direct photo link of splice tray or chamber.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAddLoopJointBox(selectedMeters, photoUrlInput, jcName)
                            showAddJointBoxDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Add Joint Box", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddJointBoxDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Bottom Action Floating Card (Route Details or Drawing Save Bar)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isDrawingMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ರಸ್ತೆಯ ಮೇಲೆ ಕೇಬಲ್ ಮಾರ್ಗವನ್ನು ಡ್ರಾ ಮಾಡಿ. Save ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSaveDrawnRoute,
                            enabled = drawPath.size >= 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_drawn_route_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Route to Shrey Network (${drawnLengthMeters}m)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (selectedRoute != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_route_bottom_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedRoute.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeDark
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedRoute.formattedLength} • ${selectedRoute.coordinates.size} Road Points • ${selectedRoute.jointBoxes.size} Joint Boxes",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF555555)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrangePrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active Route",
                                    color = OrangePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val midPoint = if (selectedRoute.coordinates.isNotEmpty()) {
                            selectedRoute.coordinates[selectedRoute.coordinates.size / 2]
                        } else GeoPoint(12.9716, 77.5946)

                        // Row 1: Navigate & Street View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("google.navigation:q=${midPoint.lat},${midPoint.lng}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(HaversineUtil.getGoogleMapsUrl(midPoint.lat, midPoint.lng)))
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = OrangeDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Navigate",
                                    fontSize = 12.sp,
                                    color = OrangeDark,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(HaversineUtil.getStreetViewUrl(midPoint.lat, midPoint.lng)))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Streetview,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoodGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Street View",
                                    fontSize = 12.sp,
                                    color = GoodGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Report Cable Cut Button
                        Button(
                            onClick = {
                                onReportCutRequested(midPoint.lat, midPoint.lng)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CutRed,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Report Cable Cut",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getMapHtmlContent(): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        html, body { height: 100%; width: 100%; margin: 0; padding: 0; background: #fff5ec; overflow: hidden; }
        
        /* Map Viewport & Rotator container to allow full 360 deg rotatable map */
        #map-viewport {
            position: relative;
            width: 100%;
            height: 100%;
            overflow: hidden;
        }
        #map-rotator {
            position: absolute;
            top: -40%;
            left: -40%;
            width: 180%;
            height: 180%;
            transform-origin: 50% 50%;
            transition: transform 0.25s ease-out;
        }
        #map {
            width: 100%;
            height: 100%;
        }

        .cut-marker {
            background-color: #ff1744;
            border: 2px solid #ffffff;
            border-radius: 50%;
            width: 18px;
            height: 18px;
            box-shadow: 0 0 10px rgba(255, 23, 68, 0.9);
            animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(0.9); opacity: 0.9; }
            50% { transform: scale(1.3); opacity: 1; }
            100% { transform: scale(0.9); opacity: 0.9; }
        }

        /* Pulsing GPS Radar Beacon for Current Location */
        .gps-current-marker {
            background-color: #2979ff;
            border: 3px solid #ffffff;
            border-radius: 50%;
            width: 18px;
            height: 18px;
            box-shadow: 0 0 0 0 rgba(41, 121, 255, 0.8);
            animation: gpsRadar 1.8s infinite;
        }
        @keyframes gpsRadar {
            0% { box-shadow: 0 0 0 0 rgba(41, 121, 255, 0.8); }
            70% { box-shadow: 0 0 0 18px rgba(41, 121, 255, 0); }
            100% { box-shadow: 0 0 0 0 rgba(41, 121, 255, 0); }
        }

        .start-pin {
            background: #00c853;
            color: #ffffff;
            font-weight: bold;
            font-size: 10px;
            padding: 3px 8px;
            border-radius: 12px;
            border: 2px solid #fff;
            box-shadow: 0 2px 5px rgba(0,0,0,0.3);
            white-space: nowrap;
        }
        .end-pin {
            background: #ff6d00;
            color: #ffffff;
            font-weight: bold;
            font-size: 10px;
            padding: 3px 8px;
            border-radius: 12px;
            border: 2px solid #fff;
            box-shadow: 0 2px 5px rgba(0,0,0,0.3);
            white-space: nowrap;
        }
        .jointbox-pin {
            background: #7c4dff;
            color: #ffffff;
            font-weight: bold;
            font-size: 9px;
            padding: 3px 6px;
            border-radius: 8px;
            border: 2px solid #fff;
            box-shadow: 0 2px 6px rgba(124, 77, 255, 0.5);
            white-space: nowrap;
            cursor: pointer;
        }
        .search-pin {
            background: #e65100;
            color: #ffffff;
            font-weight: bold;
            font-size: 10px;
            padding: 3px 8px;
            border-radius: 10px;
            border: 2px solid #fff;
            box-shadow: 0 2px 6px rgba(0,0,0,0.4);
            white-space: nowrap;
        }
        .jc-photo-thumb {
            width: 100%;
            max-height: 120px;
            object-fit: cover;
            border-radius: 6px;
            margin-top: 6px;
        }
    </style>
</head>
<body>
    <div id="map-viewport">
        <div id="map-rotator">
            <div id="map"></div>
        </div>
    </div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        var map = L.map('map', { zoomControl: false }).setView([12.9716, 77.5946], 13);
        L.control.zoom({ position: 'topleft' }).addTo(map);

        // OpenStreetMap Standard Tiles - 100% Free, Official, No API key
        var tileOsm = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        // OSM France (Clean alternate mirror)
        var tileOsmFr = L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
            maxZoom: 19,
            subdomains: 'abc',
            attribution: '© OpenStreetMap France'
        });

        // Esri Satellite
        var tileSatellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19,
            attribution: '© Esri Satellite'
        });

        var currentTileLayer = tileOsm;

        function switchTileLayer(type) {
            map.removeLayer(currentTileLayer);
            if (type === 'satellite') {
                currentTileLayer = tileSatellite;
            } else if (type === 'osmfr') {
                currentTileLayer = tileOsmFr;
            } else {
                currentTileLayer = tileOsm;
            }
            map.addLayer(currentTileLayer);
        }

        var polylineLayer = null;
        var startMarker = null;
        var endMarker = null;
        var cutMarkers = [];
        var jbMarkers = [];
        var drawPolyline = null;
        var drawMarkers = [];
        var currentLocationMarker = null;
        var searchMarker = null;
        var isDrawing = false;
        var lastBounds = null;

        map.on('click', function(e) {
            if (window.AndroidBridge) {
                window.AndroidBridge.handleMapClick(e.latlng.lat, e.latlng.lng);
            }
        });

        function setMapRotation(deg) {
            var rotator = document.getElementById('map-rotator');
            if (rotator) {
                rotator.style.transform = 'rotate(' + deg + 'deg)';
            }
            map.invalidateSize();
        }

        function updateCurrentLocation(lat, lng) {
            if (currentLocationMarker) {
                currentLocationMarker.setLatLng([lat, lng]);
            } else {
                var icon = L.divIcon({
                    className: 'gps-current-marker',
                    iconSize: [18, 18],
                    iconAnchor: [9, 9]
                });
                currentLocationMarker = L.marker([lat, lng], { icon: icon, zIndexOffset: 1000 }).addTo(map);
                currentLocationMarker.bindPopup('<b>📍 Current Location</b><br>You are here');
            }
        }

        function flyToPlace(lat, lng, name) {
            map.flyTo([lat, lng], 16, { animate: true, duration: 1.2 });
            if (searchMarker) { map.removeLayer(searchMarker); }
            var icon = L.divIcon({
                className: '',
                html: '<div class="search-pin">📍 ' + name + '</div>',
                iconSize: [100, 24],
                iconAnchor: [50, 12]
            });
            searchMarker = L.marker([lat, lng], { icon: icon }).addTo(map);
            searchMarker.bindPopup('<b>' + name + '</b><br>Lat: ' + lat.toFixed(5) + ', Lng: ' + lng.toFixed(5)).openPopup();
        }

        function clearRoute() {
            if (polylineLayer) { map.removeLayer(polylineLayer); polylineLayer = null; }
            if (startMarker) { map.removeLayer(startMarker); startMarker = null; }
            if (endMarker) { map.removeLayer(endMarker); endMarker = null; }
            jbMarkers.forEach(function(m) { map.removeLayer(m); });
            jbMarkers = [];
        }

        function renderRoute(points, name, lengthMeters, jointBoxes) {
            clearRoute();
            if (!points || points.length < 1) return;

            var latlngs = points.map(function(p) { return [p.lat, p.lng]; });

            // Glowing fiber cable polyline in vibrant telecom orange
            polylineLayer = L.polyline(latlngs, {
                color: '#ff6d00',
                weight: 5,
                opacity: 0.95,
                lineJoin: 'round'
            }).addTo(map);

            var startIcon = L.divIcon({
                className: '',
                html: '<div class="start-pin">START</div>',
                iconSize: [44, 22],
                iconAnchor: [22, 11]
            });
            startMarker = L.marker(latlngs[0], { icon: startIcon }).addTo(map);

            if (latlngs.length > 1) {
                var endIcon = L.divIcon({
                    className: '',
                    html: '<div class="end-pin">END</div>',
                    iconSize: [40, 22],
                    iconAnchor: [20, 11]
                });
                endMarker = L.marker(latlngs[latlngs.length - 1], { icon: endIcon }).addTo(map);
            }

            // Render Joint Boxes with photo option
            if (jointBoxes && jointBoxes.length > 0) {
                jointBoxes.forEach(function(jb) {
                    var jbIcon = L.divIcon({
                        className: '',
                        html: '<div class="jointbox-pin">📦 ' + (jb.loop || 20) + 'm JC</div>',
                        iconSize: [56, 20],
                        iconAnchor: [28, 10]
                    });
                    var m = L.marker([jb.lat, jb.lng], { icon: jbIcon }).addTo(map);
                    var popupHtml = '<b>' + (jb.name || 'Joint Box') + '</b><br>Loop Slack: ' + (jb.loop || 20) + ' meters';
                    if (jb.photoUrl && jb.photoUrl.length > 5) {
                        popupHtml += '<br><img src="' + jb.photoUrl + '" class="jc-photo-thumb" onerror="this.style.display=\'none\'" /><br><a href="' + jb.photoUrl + '" target="_blank" style="color:#ff6d00;font-size:11px;font-weight:bold;">View Photo</a>';
                    }
                    m.bindPopup(popupHtml);
                    jbMarkers.push(m);
                });
            }

            lastBounds = polylineLayer.getBounds();
            map.fitBounds(lastBounds, { padding: [50, 50] });
        }

        function fitBoundsToCurrent() {
            if (lastBounds) {
                map.fitBounds(lastBounds, { padding: [40, 40] });
            } else if (polylineLayer) {
                map.fitBounds(polylineLayer.getBounds(), { padding: [40, 40] });
            } else if (currentLocationMarker) {
                map.setView(currentLocationMarker.getLatLng(), 15);
            } else {
                map.setView([12.9716, 77.5946], 13);
            }
        }

        function renderCuts(cuts) {
            cutMarkers.forEach(function(m) { map.removeLayer(m); });
            cutMarkers = [];

            if (!cuts) return;
            cuts.forEach(function(c) {
                var icon = L.divIcon({
                    className: 'cut-marker',
                    iconSize: [18, 18],
                    iconAnchor: [9, 9]
                });
                var marker = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);
                marker.bindPopup('<b>Cut: ' + (c.name || 'Route') + '</b><br>' + (c.notes || 'Reported'));
                marker.on('click', function() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.handleMarkerClick(c.name || 'Cut Location', c.lat, c.lng);
                    }
                });
                cutMarkers.push(marker);
            });
        }

        function setDrawingMode(enabled, pathPoints, jointBoxes) {
            isDrawing = enabled;
            if (drawPolyline) { map.removeLayer(drawPolyline); drawPolyline = null; }
            drawMarkers.forEach(function(m) { map.removeLayer(m); });
            drawMarkers = [];

            if (enabled && pathPoints && pathPoints.length > 0) {
                var latlngs = pathPoints.map(function(p) { return [p.lat, p.lng]; });
                drawPolyline = L.polyline(latlngs, {
                    color: '#ff6d00',
                    weight: 5,
                    dashArray: '8, 8'
                }).addTo(map);

                // Start badge
                var startIcon = L.divIcon({
                    className: '',
                    html: '<div class="start-pin">START</div>',
                    iconSize: [44, 22],
                    iconAnchor: [22, 11]
                });
                var sm = L.marker(latlngs[0], { icon: startIcon }).addTo(map);
                drawMarkers.push(sm);

                // Waypoint markers
                for (var i = 1; i < latlngs.length; i++) {
                    var m = L.circleMarker(latlngs[i], {
                        radius: 5,
                        color: '#e65100',
                        fillColor: '#ff9100',
                        fillOpacity: 1
                    }).addTo(map);
                    drawMarkers.push(m);
                }

                // Render Joint Boxes in draw mode with photo link preview
                if (jointBoxes && jointBoxes.length > 0) {
                    jointBoxes.forEach(function(jb) {
                        var jbIcon = L.divIcon({
                            className: '',
                            html: '<div class="jointbox-pin">📦 ' + (jb.loop || 20) + 'm JC</div>',
                            iconSize: [56, 20],
                            iconAnchor: [28, 10]
                        });
                        var jbm = L.marker([jb.lat, jb.lng], { icon: jbIcon }).addTo(map);
                        var pContent = '<b>' + (jb.name || 'Joint Box') + '</b><br>Loop Slack: ' + (jb.loop || 20) + 'm';
                        if (jb.photoUrl && jb.photoUrl.length > 5) {
                            pContent += '<br><img src="' + jb.photoUrl + '" class="jc-photo-thumb" onerror="this.style.display=\'none\'" />';
                        }
                        jbm.bindPopup(pContent);
                        drawMarkers.push(jbm);
                    });
                }

                // Follow last point
                var last = latlngs[latlngs.length - 1];
                map.panTo(last);
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}
