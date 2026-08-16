package com.arielfaridja.ezrahi.domain.model

// 1. Roles available in the field
enum class UserRole {
    MANAGER,      // מנהל פעילות - Full permissions
    LEAD_GUIDE,   // מוביל - Trail navigation
    SWEEP_GUIDE,  // מאסף - Rear guard safety
    MEDIC,        // חובש - First response
    LOGISTICS,    // לוגיסטיקה - Food, water, gear
    MEMBER        // חניך / משתתף - Regular participant
}

// 2. Location coordinates
data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

// 3. User representation
data class UserProfile(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = ""
)

// 4. Participant inside a specific field event
data class EventParticipant(
    val userId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.MEMBER,
    val currentLocation: GeoPoint? = null,
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

// 5. The primary Field Event (Formerly "Activity")
data class FieldEvent(
    val id: String = "",
    val name: String = "",
    val managerId: String = "",
    val managerContact: String = "",
    val gpxRouteUrl: String? = null,
    val isLive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// 6. Messages sent in field channels
data class FieldMessage(
    val id: String = "",
    val eventId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: UserRole = UserRole.MEMBER,
    val targetRole: UserRole? = null, // null means broadcast to all
    val messageText: String = "",
    val isEmergency: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// 7. Report status (maps to legacy ReportStatus values: 1/2/-1)
enum class FieldReportStatus(val value: Int) {
    REPORTED(1),
    HANDLED(2),
    UNKNOWN(-1);

    companion object {
        fun getByValue(value: Int): FieldReportStatus = when (value) {
            1 -> REPORTED
            2 -> HANDLED
            else -> UNKNOWN
        }
    }
}

// 8. Report type (maps to legacy ReportType values: 0/1/-1)
enum class FieldReportType(val value: Int) {
    GENERAL(0),
    MEDICAL(1),
    UNKNOWN(-1);

    companion object {
        fun getByValue(value: Int): FieldReportType = when (value) {
            0 -> GENERAL
            1 -> MEDICAL
            else -> UNKNOWN
        }
    }
}

// 9. Field report (formerly "Report") - e.g. an incident or observation at a location
data class FieldReport(
    val id: String = "",
    val actId: String = "",
    val reporterId: String = "",
    val title: String = "",
    val description: String = "",
    val location: GeoPoint = GeoPoint(),
    val reportTime: Long = System.currentTimeMillis(),
    val status: FieldReportStatus = FieldReportStatus.REPORTED,
    val type: FieldReportType = FieldReportType.GENERAL
)
