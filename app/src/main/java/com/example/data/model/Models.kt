package com.example.data.model

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class CableRoute(
    val id: String,
    val name: String,
    val coordinates: List<GeoPoint>,
    val lengthMeters: Long,
    val createdBy: String,
    val createdAt: String
) {
    val formattedLength: String
        get() = if (lengthMeters >= 1000) {
            String.format("%.2f km", lengthMeters / 1000.0)
        } else {
            "$lengthMeters m"
        }
}

data class CutReport(
    val id: String,
    val routeId: String,
    val routeName: String,
    val lat: Double,
    val lng: Double,
    val reportedBy: String,
    val timestamp: String,
    val mapsLink: String,
    val streetViewLink: String,
    val notes: String = "",
    val status: String = "Open"
)

enum class UserRole {
    ADMIN,
    TECHNICIAN
}

data class UserSession(
    val phone: String,
    val role: UserRole
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
}
