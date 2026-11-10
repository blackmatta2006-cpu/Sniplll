package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CutReport
import com.example.ui.theme.CutRed
import com.example.ui.theme.GoodGreen
import com.example.ui.theme.OrangeDark
import com.example.ui.theme.OrangePrimary

@Composable
fun CutHistoryScreen(
    cutReports: List<CutReport>,
    onViewOnMapClick: (CutReport) -> Unit,
    onDeleteCutReport: (CutReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var incidentToDelete by remember { mutableStateOf<CutReport?>(null) }

    if (cutReports.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFFF9F5))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(GoodGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GoodGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Cable Cuts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2420)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "All surveyed routes are operational. Cut reports submitted by technicians will appear here.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 18.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFFF9F5))
                .testTag("cut_history_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reported Cable Incidents (${cutReports.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OrangeDark
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CutRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Requires Attention",
                            color = CutRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            items(cutReports, key = { it.id }) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cut_report_card_${report.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0CC))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(CutRed.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = null,
                                        tint = CutRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = report.routeName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2C2420)
                                        )
                                    )
                                    Text(
                                        text = "Reported by: ${report.reportedBy}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = "Cut Alert",
                                    color = CutRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // GPS Coordinates
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "GPS: ${String.format(java.util.Locale.US, "%.6f, %.6f", report.lat, report.lng)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF333333)
                                    )
                                )
                                if (report.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Notes: ${report.notes}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Navigate, Street View, Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(report.mapsLink))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeDark),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(report.streetViewLink))
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoodGreen),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Streetview,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Street View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (report.reportedBy.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${report.reportedBy}"))
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call technician",
                                        tint = OrangeDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Delete Incident Option
                            IconButton(
                                onClick = { incidentToDelete = report },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(CutRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .testTag("delete_incident_${report.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Incident",
                                    tint = CutRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Incident Confirmation Dialog
        if (incidentToDelete != null) {
            val incident = incidentToDelete!!
            AlertDialog(
                onDismissRequest = { incidentToDelete = null },
                title = {
                    Text(
                        text = "Delete Incident / ಇನ್ಸಿಡೆಂಟ್ ಡಿಲೀಟ್",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CutRed)
                    )
                },
                text = {
                    Text("Delete cut report on '${incident.routeName}' (${incident.notes})? This will permanently remove it from the system.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCutReport(incident)
                            incidentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CutRed)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { incidentToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
