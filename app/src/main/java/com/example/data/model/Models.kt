package com.example.data.model

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class LoopJointBox(
    val id: String,
    val lat: Double,
    val lng: Double,
    val loopMeters: Int = 20, // default 20m cable slack reserve in joint closure
    val name: String = "Loop Joint Box"
)

data class CableRoute(
    val id: String,
    val name: String,
    val coordinates: List<GeoPoint>,
    val lengthMeters: Long,
    val createdBy: String,
    val createdAt: String,
    val jointBoxes: List<LoopJointBox> = emptyList()
) {
    val totalCableLengthMeters: Long
        get() {
            val loopTotal = jointBoxes.sumOf { it.loopMeters }
            return lengthMeters + loopTotal
        }

    val formattedLength: String
        get() {
            val total = totalCableLengthMeters
            return if (total >= 1000) {
                String.format("%.2f km", total / 1000.0)
            } else {
                "$total m"
            }
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

enum class DrawDirection(val label: String, val iconText: String, val bearingDelta: Double) {
    STRAIGHT("Straight / ಮುಂದೆ", "⬆️", 0.0),
    RIGHT("Right / ಬಲಕ್ಕೆ", "➡️", 90.0),
    LEFT("Left / ಎಡಕ್ಕೆ", "⬅️", -90.0),
    BACK("Back / ಹಿಂದೆ", "⬇️", 180.0)
}
