package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.GeoPoint
import com.example.data.model.UserSession
import com.example.ui.theme.CutRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FiberCyan
import com.example.ui.theme.GoodGreen
import com.example.ui.theme.TelecomNavy
import com.example.util.HaversineUtil
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
    drawnLengthMeters: Long,
    onAddDrawPoint: (GeoPoint) -> Unit,
    onUndoDrawPoint: () -> Unit,
    onSaveDrawnRoute: () -> Unit,
    onCancelDrawing: () -> Unit,
    onReportCutRequested: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var selectedMarkerInfo by remember { mutableStateOf<String?>(null) }
    var showTileSwitcher by remember { mutableStateOf(false) }
    var activeTileType by remember { mutableStateOf("osm") } // "osm", "satellite", "topo"

    val mapHtml = remember { getMapHtmlContent() }

    // Update Route and Cut Pins on Map when selection or data changes
    LaunchedEffect(isMapLoaded, selectedRoute, cutReports, isDrawingMode, drawPath, activeTileType) {
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
            wv.evaluateJavascript("setDrawingMode(true, $coordsJson);", null)
        } else {
            wv.evaluateJavascript("setDrawingMode(false, []);", null)

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
                val nameEscaped = JSONObject.quote(selectedRoute.name)
                wv.evaluateJavascript(
                    "renderRoute($coordsJson, $nameEscaped, ${selectedRoute.lengthMeters});",
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

    Box(modifier = modifier.fillMaxSize()) {
        // Full Page Edge-to-Edge WebView
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

                    loadDataWithBaseURL("https://osm.org", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_page_map_webview")
        )

        // Top Floating Info Bar (Drawing Mode or Selected Route Stats)
        if (isDrawingMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(16.dp),
                color = TelecomNavy,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Surveying / Drawing Route",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = FiberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${drawPath.size} points • ${drawnLengthMeters}m total",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onUndoDrawPoint,
                            enabled = drawPath.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("undo_draw_point_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo point",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onCancelDrawing,
                            modifier = Modifier
                                .size(38.dp)
                                .background(CutRed.copy(alpha = 0.3f), CircleShape)
                                .testTag("cancel_drawing_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel drawing",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Map Control Floating Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter on Route
            FloatingActionButton(
                onClick = {
                    webViewRef?.evaluateJavascript("fitBoundsToCurrent();", null)
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("fit_bounds_button"),
                containerColor = Color.White,
                contentColor = TelecomNavy,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Fit to Route",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Layer Switcher
            FloatingActionButton(
                onClick = {
                    showTileSwitcher = !showTileSwitcher
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_layers_button"),
                containerColor = Color.White,
                contentColor = ElectricBlue,
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
                shape = RoundedCornerShape(12.dp),
                color = TelecomNavy,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Map Tile Style",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "osm" to "OpenStreetMap (Standard)",
                        "satellite" to "Satellite / Imagery",
                        "topo" to "Topographic / Terrain"
                    ).forEach { (type, label) ->
                        Button(
                            onClick = {
                                activeTileType = type
                                showTileSwitcher = false
                                webViewRef?.evaluateJavascript("switchTileLayer('$type');", null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTileType == type) FiberCyan else Color.White.copy(alpha = 0.15f),
                                contentColor = if (activeTileType == type) TelecomNavy else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            }
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
                    colors = CardDefaults.cardColors(containerColor = TelecomNavy),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tap on map to add cable waypoints. Minimum 2 points required.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onSaveDrawnRoute,
                            enabled = drawPath.size >= 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_drawn_route_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FiberCyan,
                                contentColor = TelecomNavy,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Route to Database (${drawnLengthMeters}m)", fontWeight = FontWeight.Bold)
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
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        color = TelecomNavy
                                    )
                                )
                                Text(
                                    text = "${selectedRoute.formattedLength} • ${selectedRoute.coordinates.size} Waypoints",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF555555)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricBlue.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active Route",
                                    color = ElectricBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Google Maps Navigation, Street View, Report Cut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val midPoint = if (selectedRoute.coordinates.isNotEmpty()) {
                                selectedRoute.coordinates[selectedRoute.coordinates.size / 2]
                            } else GeoPoint(12.9716, 77.5946)

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
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ElectricBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontSize = 12.sp, color = ElectricBlue)
                            }

                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(HaversineUtil.getStreetViewUrl(midPoint.lat, midPoint.lng)))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Streetview,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoodGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Street View", fontSize = 12.sp, color = GoodGreen)
                            }

                            Button(
                                onClick = {
                                    onReportCutRequested(midPoint.lat, midPoint.lng)
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CutRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report Cut", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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
        html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e5e5e5; }
        .cut-marker {
            background-color: #ff1744;
            border: 2px solid #ffffff;
            border-radius: 50%;
            width: 18px;
            height: 18px;
            box-shadow: 0 0 10px rgba(255, 23, 68, 0.8);
            animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(0.9); opacity: 0.9; }
            50% { transform: scale(1.3); opacity: 1; }
            100% { transform: scale(0.9); opacity: 0.9; }
        }
        .start-pin {
            background: #00c853;
            color: #ffffff;
            font-weight: bold;
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 12px;
            border: 2px solid #fff;
            white-space: nowrap;
        }
        .end-pin {
            background: #1a237e;
            color: #ffffff;
            font-weight: bold;
            font-size: 10px;
            padding: 2px 6px;
            border-radius: 12px;
            border: 2px solid #fff;
            white-space: nowrap;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        var map = L.map('map', { zoomControl: false }).setView([12.9716, 77.5946], 13);
        L.control.zoom({ position: 'topleft' }).addTo(map);

        var tileOsm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OSM'
        }).addTo(map);

        var tileSatellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19
        });

        var tileTopo = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
            maxZoom: 17
        });

        var currentTileLayer = tileOsm;

        function switchTileLayer(type) {
            map.removeLayer(currentTileLayer);
            if (type === 'satellite') {
                currentTileLayer = tileSatellite;
            } else if (type === 'topo') {
                currentTileLayer = tileTopo;
            } else {
                currentTileLayer = tileOsm;
            }
            map.addLayer(currentTileLayer);
        }

        var polylineLayer = null;
        var startMarker = null;
        var endMarker = null;
        var cutMarkers = [];
        var drawPolyline = null;
        var drawMarkers = [];
        var isDrawing = false;
        var lastBounds = null;

        map.on('click', function(e) {
            if (window.AndroidBridge) {
                window.AndroidBridge.handleMapClick(e.latlng.lat, e.latlng.lng);
            }
        });

        function clearRoute() {
            if (polylineLayer) { map.removeLayer(polylineLayer); polylineLayer = null; }
            if (startMarker) { map.removeLayer(startMarker); startMarker = null; }
            if (endMarker) { map.removeLayer(endMarker); endMarker = null; }
        }

        function renderRoute(points, name, lengthMeters) {
            clearRoute();
            if (!points || points.length < 1) return;

            var latlngs = points.map(function(p) { return [p.lat, p.lng]; });

            // Glowing outline + main fiber cable line
            polylineLayer = L.polyline(latlngs, {
                color: '#0066ff',
                weight: 5,
                opacity: 0.9,
                lineJoin: 'round'
            }).addTo(map);

            var startIcon = L.divIcon({
                className: '',
                html: '<div class="start-pin">START</div>',
                iconSize: [40, 20],
                iconAnchor: [20, 10]
            });
            startMarker = L.marker(latlngs[0], { icon: startIcon }).addTo(map);

            if (latlngs.length > 1) {
                var endIcon = L.divIcon({
                    className: '',
                    html: '<div class="end-pin">END</div>',
                    iconSize: [35, 20],
                    iconAnchor: [17, 10]
                });
                endMarker = L.marker(latlngs[latlngs.length - 1], { icon: endIcon }).addTo(map);
            }

            lastBounds = polylineLayer.getBounds();
            map.fitBounds(lastBounds, { padding: [50, 50] });
        }

        function fitBoundsToCurrent() {
            if (lastBounds) {
                map.fitBounds(lastBounds, { padding: [40, 40] });
            } else if (polylineLayer) {
                map.fitBounds(polylineLayer.getBounds(), { padding: [40, 40] });
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

        function setDrawingMode(enabled, pathPoints) {
            isDrawing = enabled;
            if (drawPolyline) { map.removeLayer(drawPolyline); drawPolyline = null; }
            drawMarkers.forEach(function(m) { map.removeLayer(m); });
            drawMarkers = [];

            if (enabled && pathPoints && pathPoints.length > 0) {
                var latlngs = pathPoints.map(function(p) { return [p.lat, p.lng]; });
                drawPolyline = L.polyline(latlngs, {
                    color: '#00e5ff',
                    weight: 4,
                    dashArray: '6, 6'
                }).addTo(map);

                latlngs.forEach(function(ll, idx) {
                    var m = L.circleMarker(ll, {
                        radius: 6,
                        color: '#1a237e',
                        fillColor: '#00e5ff',
                        fillOpacity: 1
                    }).addTo(map);
                    drawMarkers.push(m);
                });

                if (latlngs.length === 1) {
                    map.panTo(latlngs[0]);
                }
            }
        }
    </script>
</body>
</html>
    """.trimIndent()
}
