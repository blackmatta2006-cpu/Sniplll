package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.CableRoute
import com.example.data.model.CutReport
import com.example.data.model.UserSession
import com.example.ui.theme.CutRed
import com.example.ui.theme.GoodGreen
import com.example.ui.theme.JointBoxPurple
import com.example.ui.theme.OrangeAmber
import com.example.ui.theme.OrangeDark
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.OrangePrimary

@Composable
fun RoutesListScreen(
    routes: List<CableRoute>,
    selectedRoute: CableRoute?,
    cutReports: List<CutReport>,
    userSession: UserSession?,
    searchQuery: String,
    onRouteSelected: (CableRoute, Boolean) -> Unit,
    onDeleteRoute: (CableRoute) -> Unit,
    onStartDrawRoute: () -> Unit,
    onReportCutForRoute: (CableRoute) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var routeToDelete by remember { mutableStateOf<CableRoute?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9F5))
    ) {
        if (routes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(OrangePrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = OrangePrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No routes match '$searchQuery'" else "No routes yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C2420)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (searchQuery.isNotEmpty())
                        "Try a different search query or clear the filter."
                    else
                        "Start surveying by drawing along the road or driving with auto-tracking.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClearSearch,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Clear Search Filter")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("routes_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header summary item
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shrey Network Routes (${routes.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OrangeDark
                            )
                        )
                        if (selectedRoute != null) {
                            Text(
                                text = "Active: ${selectedRoute.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                items(routes, key = { it.id }) { route ->
                    val isSelected = selectedRoute?.id == route.id
                    val cutsForThisRoute = cutReports.filter { it.routeId == route.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRouteSelected(route, false) }
                            .testTag("route_card_${route.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) OrangePrimary else Color(0xFFFFE0CC)
                        ),
                        elevation = CardDefaults.cardElevation(if (isSelected) 5.dp else 1.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Name and Length Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isSelected) OrangePrimary else OrangePrimary.copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timeline,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else OrangeDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = route.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2C2420)
                                            )
                                        )
                                        Text(
                                            text = "Created by: ${route.createdBy}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF757575))
                                        )
                                    }
                                }

                                // Total length pill (including joint box slack)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = OrangePrimary
                                ) {
                                    Text(
                                        text = route.formattedLength,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Badges: Road Points, Joint Boxes, Operational status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFECE0)
                                ) {
                                    Text(
                                        text = "${route.coordinates.size} Road Points",
                                        color = OrangeDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (route.jointBoxes.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = JointBoxPurple.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = JointBoxPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${route.jointBoxes.size} JC (${route.jointBoxes.sumOf { it.loopMeters }}m)",
                                                color = JointBoxPurple,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (cutsForThisRoute.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CutRed.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReportProblem,
                                                contentDescription = null,
                                                tint = CutRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${cutsForThisRoute.size} cut${if (cutsForThisRoute.size > 1) "s" else ""}",
                                                color = CutRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoodGreen.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Operational",
                                            color = GoodGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons: Full Map, Cut Report, and Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onRouteSelected(route, true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OrangePrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Full Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onReportCutForRoute(route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CutRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = CutRed
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cut Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Delete Route Option (for Admins)
                                if (userSession?.isAdmin == true) {
                                    IconButton(
                                        onClick = { routeToDelete = route },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(CutRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                            .testTag("delete_route_${route.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Route",
                                            tint = CutRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (routeToDelete != null) {
            val r = routeToDelete!!
            AlertDialog(
                onDismissRequest = { routeToDelete = null },
                title = {
                    Text(
                        text = "Delete Route?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CutRed)
                    )
                },
                text = {
                    Text("Are you sure you want to delete '${r.name}' (${r.formattedLength})? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteRoute(r)
                            routeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CutRed)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { routeToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Floating Action Button to Draw Route (Admin)
        if (userSession?.isAdmin == true) {
            ExtendedFloatingActionButton(
                onClick = onStartDrawRoute,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_draw_fab"),
                containerColor = OrangePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.EditLocation, contentDescription = null) },
                text = { Text("Draw Road Route", fontWeight = FontWeight.Bold) }
            )
        }
    }
}
