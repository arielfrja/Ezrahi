
# 🚨 SYSTEM DIRECTIVE & CODE AUDIT: FIXING REGRESSIONS & COMPLETING EZRAHI MODERNIZATION
**Target Branch:** `refactor/modernization-v2`  
**Repository:** `https://github.com/arielfrja/Ezrahi`  
**Role:** Principal Android Architect, Lead Product Engineer, and AppSec Lead

---

## ✅ Progress Tracker

| # | Item | Status | Where |
|---|---|---|---|
| F1 | Quick-Dial Screen (`QuickDialFragment`) | 🔵 TODO | `todo-fix-2.md` |
| F2 | Event Management Screen (`ActivityOverviewFragment`) | 🔵 TODO | `todo-fix-3.md` |
| F3 | GPX Route Parser + Polyline Renderer | 🔵 TODO | `todo-fix-5.md` |
| F4 | Structured Field Reports & Checkpoints | 🟢 DONE (Phase 5: long-press → Add Marker dialog → `Reports`; SOS FAB removed by PO decision, `triggerSOS` dormant) | `todo-fix-1` series |
| F5 | Full User Registration (name/email/phone → `Users/{uid}`) | 🟢 DONE (Phase 5 signup + `registerUser`) | Phase 5 |
| F6 | Role-Based Messaging Channels | 🔵 TODO | `todo-fix-4.md` |
| B1 | Firestore SnapshotListener Leaks | 🟢 DONE (`callbackFlow`/`awaitClose` on all 7 listeners, commit `30984f5`) | `todo-fix-1.md` |
| B2 | Android 14+ FGS Crash | 🟢 DONE (Phase 4: `FOREGROUND_SERVICE_LOCATION` declared + API 34 `startForeground` branch + POST_NOTIFICATIONS flow) | Phase 4 |
| B3 | Room Type Handling | 🟢 N/A — all Room fields are primitives; no converters needed | — |
| B4 | Runtime Location Permissions in Compose | 🟢 DONE (Phase 4 + MapScreen: fine/coarse/background/battery + consent dialogs) | Phase 4 |

---

## 1. CRITICAL AUDIT: WHAT WAS COMPROMISED OR MISSED
A thorough audit of the modernization output indicates that while the high-level framework (Compose, Room, Hilt) was initiated, the implementation is **incomplete, contains severe functional regressions, and drops core features from the original Ezrahi specification**.

### ⚠️ Functional Regressions (Features Dropped from the Original App):
1. **Missing Quick-Dial Screen (`QuickDialFragment`):** 
   - *Original Requirement:* A dedicated role-based emergency and contact dialer where guides and participants can instantly place phone calls to specific role-holders (e.g., Lead Guide, Medic, Sweep Guide, Basecamp) without knowing their personal phone numbers in advance.
   - *Current Status:* **COMPLETELY MISSING** from navigation and UI.
2. **Missing Activity / Event Management Screen (`ActivityOverviewFragment`):**
   - *Original Requirement:* Event managers must be able to create/edit events, view and assign role permissions (`UserRole` / `ActPermission`), upload and switch GPX routes, and inspect field report checkpoints.
   - *Current Status:* **DROPPED.** The app currently navigates to a hardcoded `"map/demo_event_123"` with no event selection, event creation, or management screen.
3. **Missing GPX Route Parser & Polyline Renderer:**
   - *Original Requirement:* Uploading, parsing, and rendering `.gpx` track files on top of the Google Map to guide groups along the designated trail.
   - *Current Status:* Only point markers exist; GPX XML parsing, file picking, and `Polyline` rendering in Compose are missing.
4. **Missing Field Reports & Checkpoints ("דיווחים מהשטח / נקודות דיווח"):**
   - *Original Requirement:* Guides can drop timed status reports, hazard alerts (e.g., blocked trail, heat exhaustion, water point empty), and checkpoints with coordinate tags.
   - *Current Status:* Reduced to a generic SOS button with no structured reporting UI.
5. **Incomplete User Registration (`Signup`):**
   - *Original Requirement:* First Name, Last Name, Email, and Phone Number must be gathered and persisted into a `users` Firestore document and Room cache.
   - *Current Status:* Only raw email/password authentication is executed; user metadata is lost.
6. **Superficial Role-Based Messaging:**
   - *Original Requirement:* Separate WhatsApp-style channels filtered by role (e.g., "Medics Only", "Staff Only", "All Broadcast").
   - *Current Status:* A single flat stream without role-based channel filtering or sending permissions.

---

### ⚠️ Critical Technical & Runtime Bugs in Current Code:
1. **Firestore SnapshotListener Memory Leak:**
   - `addSnapshotListener` was called inside repository methods without lifecycle management or cancellation. Every time a screen opens or rotates, duplicate listeners attach, leaking memory and multiplying Firebase read costs exponentially.
   - *Required Fix:* Must wrap all Firestore queries in `callbackFlow { ... awaitClose { listener.remove() } }`.
2. **Android 14+ (API 34) Foreground Service Crash:**
   - Calling `startForeground()` without explicitly requesting `POST_NOTIFICATIONS` and `FOREGROUND_SERVICE_LOCATION` at runtime will trigger a fatal `SecurityException` on Android 13/14.
3. **Room Database Type Handling:**
   - Room cannot persist complex objects (`UserRole`, `GeoPoint`, `List<...>`) without `@TypeConverters`.
4. **Missing Runtime Permission Handling in Compose:**
   - The map screen renders before location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) are granted, resulting in a blank map and silent failures.

---

## 2. INSTRUCTIONS FOR THE AI AGENT: ARCHITECTURE SPECIFICATION

You must refactor and complete the codebase according to strict Clean Architecture:

```
app/src/main/java/com/arielfaridja/ezrahi/
├── di/                     # Hilt Modules (AppModule, DatabaseModule, FirebaseModule, RepositoryModule)
├── data/
│   ├── local/             # Room Database, DAOs, Entity Models, TypeConverters
│   ├── remote/            # Firebase Data Source & Firestore/Storage Wrappers
│   ├── parser/            # GPX XML Track Parser
│   └── repository/        # Offline-First Repository Implementations
├── domain/
│   ├── model/             # Pure Kotlin Domain Data Classes & Enums
│   ├── repository/        # Repository Interfaces
│   └── mesh/              # LoRa / Hardware Transceiver Abstraction
├── service/               # LocationTrackingService (Foreground Service)
└── ui/
    ├── theme/             # Material 3 Theme, Typography, RTL support
    ├── navigation/        # Type-safe / String-route NavHost
    ├── auth/              # Login & Signup Screens (Full User Profile)
    ├── event/             # Event List, Creation, and Event Overview/Management Screens
    ├── map/               # Live Map with GPX Polyline, Role Markers & SOS FAB
    ├── chat/              # Role-Filtered Messaging Screen
    ├── dial/              # Role-Based Quick Dial Screen
    └── report/            # Field Incident & Checkpoint Reporting Dialog/Screen
```

---

## 3. STEP-BY-STEP REFACTORING & CODE BLUEPRINTS

### Step 1: Fix Room Type Converters & Schema
Create a robust Type Converter for Room to store Enums, Dates, and Locations.

📁 **`data/local/Converters.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.local

import androidx.room.TypeConverter
import com.arielfaridja.ezrahi.domain.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.MEMBER)
}
```
Add `@TypeConverters(Converters::class)` to `EzrahiDatabase.kt`.

---

### Step 2: Implement Leak-Free, Reactive Repository with `callbackFlow`
Replace all leaky Firestore listeners with structured `callbackFlow` pipelines.

📁 **`data/repository/EzrahiRepositoryImpl.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.repository

import com.arielfaridja.ezrahi.data.local.*
import com.arielfaridja.ezrahi.domain.model.*
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EzrahiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dao: EzrahiDao
) : EzrahiRepository {

    override fun observeEvent(eventId: String): Flow<FieldEvent?> = callbackFlow {
        val docRef = firestore.collection("events").document(eventId)
        val registration: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val event = EventLocalEntity(
                    id = snapshot.id,
                    name = snapshot.getString("name") ?: "",
                    managerId = snapshot.getString("managerId") ?: "",
                    managerContact = snapshot.getString("managerContact") ?: "",
                    gpxRouteUrl = snapshot.getString("gpxRouteUrl"),
                    isLive = snapshot.getBoolean("isLive") ?: true
                )
                trySend(event)
            }
        }
        awaitClose { registration.remove() }
    }.map { local ->
        local?.let {
            FieldEvent(
                id = it.id,
                name = it.name,
                managerId = it.managerId,
                managerContact = it.managerContact,
                gpxRouteUrl = it.gpxRouteUrl,
                isLive = it.isLive
            )
        }
    }

    override fun observeParticipants(eventId: String): Flow<List<EventParticipant>> = callbackFlow {
        val collRef = firestore.collection("events").document(eventId).collection("participants")
        val registration = collRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.map { doc ->
                    EventParticipant(
                        userId = doc.id,
                        fullName = doc.getString("fullName") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        role = runCatching { UserRole.valueOf(doc.getString("role") ?: "") }.getOrDefault(UserRole.MEMBER),
                        currentLocation = GeoPoint(
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            timestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                        ),
                        isOnline = doc.getBoolean("isOnline") ?: true,
                        lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun observeMessages(eventId: String, targetRole: UserRole?): Flow<List<FieldMessage>> = callbackFlow {
        val query = firestore.collection("events").document(eventId).collection("messages")
            .orderBy("timestamp")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    val senderRole = runCatching { UserRole.valueOf(doc.getString("senderRole") ?: "") }.getOrDefault(UserRole.MEMBER)
                    val rawTarget = doc.getString("targetRole")
                    val msgTarget = rawTarget?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

                    // Role filter logic
                    if (targetRole == null || msgTarget == null || msgTarget == targetRole) {
                        FieldMessage(
                            id = doc.id,
                            eventId = eventId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderRole = senderRole,
                            targetRole = msgTarget,
                            messageText = doc.getString("messageText") ?: "",
                            isEmergency = doc.getBoolean("isEmergency") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } else null
                }
                trySend(messages)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit> = runCatching {
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastSeenTimestamp" to location.timestamp,
            "isOnline" to true
        )
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    override suspend fun sendFieldReport(report: FieldReport): Result<Unit> = runCatching {
        firestore.collection("events").document(report.eventId)
            .collection("reports").document(report.id.ifEmpty { firestore.collection("events").document().id })
            .set(report).await()
    }

    override suspend fun sendMessage(message: FieldMessage): Result<Unit> = runCatching {
        firestore.collection("events").document(message.eventId)
            .collection("messages").document(message.id.ifEmpty { firestore.collection("events").document().id })
            .set(message).await()
    }

    override suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit> = runCatching {
        val sos = FieldMessage(
            id = "SOS_${System.currentTimeMillis()}",
            eventId = eventId,
            senderId = senderId,
            senderName = senderName,
            senderRole = UserRole.MEMBER,
            targetRole = null,
            messageText = "🚨 EMERGENCY SOS! Participant requires urgent support at (${location.latitude}, ${location.longitude})",
            isEmergency = true,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(sos).getOrThrow()
    }
}
```

---

### Step 3: Implement GPX Route XML Parser
Build a standalone GPX parser so trails can be loaded from file/URL and drawn as polylines on Google Maps.

📁 **`data/parser/GpxParser.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.parser

import com.google.android.gms.maps.model.LatLng
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object GpxParser {
    fun parseGpx(inputStream: InputStream): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            if (eventType == XmlPullParser.START_TAG && (name.equals("trkpt", true) || name.equals("wpt", true))) {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    points.add(LatLng(lat, lon))
                }
            }
            eventType = parser.next()
        }
        return points
    }
}
```

---

### Step 4: Re-Implement Missing Screen: Quick Dial by Role (`QuickDialScreen.kt`)
Enables direct phone calls to staff roles (Medic, Lead, Tail, Logistics) without prior number discovery.

📁 **`ui/dial/QuickDialScreen.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.dial

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDialScreen(
    participants: List<EventParticipant>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Dial / חיוג מהיר לבעלי תפקידים") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(participants.filter { it.role != UserRole.MEMBER && it.phoneNumber.isNotBlank() }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contact.phoneNumber}")
                            }
                            context.startActivity(callIntent)
                        },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (contact.role) {
                                    UserRole.MEDIC -> Icons.Default.MedicalServices
                                    UserRole.LEAD_GUIDE -> Icons.Default.Navigation
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(contact.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(contact.role.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        IconButton(onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contact.phoneNumber}")
                            }
                            context.startActivity(callIntent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
```

---

### Step 5: Re-Implement Missing Screen: Activity / Event Overview & Management (`EventManagementScreen.kt`)
Enables the manager to manage participants, check routes, assign roles, and view field status.

📁 **`ui/event/EventManagementScreen.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    event: FieldEvent?,
    participants: List<EventParticipant>,
    isManager: Boolean,
    onNavigateToMap: () -> Unit,
    onNavigateToQuickDial: () -> Unit,
    onNavigateToChat: () -> Unit,
    onUploadGpxRoute: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "Event Overview / ניהול פעילות") },
                actions = {
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Open Map")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Event Details Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activity: ${event?.name ?: "Loading..."}", style = MaterialTheme.typography.titleLarge)
                    Text("Manager Contact: ${event?.managerContact ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onNavigateToQuickDial) { Text("Quick Dial / חיוג מהיר") }
                        Button(onClick = onNavigateToChat) { Text("Messages / מסרים") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isManager) {
                OutlinedButton(
                    onClick = onUploadGpxRoute,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload / Change GPX Route (העלאת מסלול)")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Staff & Participants (${participants.size})", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants) { participant ->
                    ListItem(
                        headlineContent = { Text(participant.fullName) },
                        supportingContent = { Text("Role: ${participant.role} • ${participant.phoneNumber}") },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            Text(
                                if (participant.isOnline) "● Live" else "○ Offline",
                                color = if (participant.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
```

---

### Step 6: Complete Navigation Graph in `MainActivity.kt`
Hook up all original routes so no feature is left orphaned.

📁 **`ui/navigation/EzrahiNavHost.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arielfaridja.ezrahi.ui.auth.AuthScreen
import com.arielfaridja.ezrahi.ui.dial.QuickDialScreen
import com.arielfaridja.ezrahi.ui.event.EventManagementScreen
import com.arielfaridja.ezrahi.ui.map.MapScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object EventOverview : Screen("event_overview/{eventId}") {
        fun createRoute(eventId: String) = "event_overview/$eventId"
    }
    object Map : Screen("map/{eventId}") {
        fun createRoute(eventId: String) = "map/$eventId"
    }
    object QuickDial : Screen("quick_dial/{eventId}") {
        fun createRoute(eventId: String) = "quick_dial/$eventId"
    }
    object Chat : Screen("chat/{eventId}") {
        fun createRoute(eventId: String) = "chat/$eventId"
    }
}

@Composable
fun EzrahiNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Auth.route) {
            AuthScreen(onAuthSuccess = {
                // Navigate to active event overview
                navController.navigate(Screen.EventOverview.createRoute("demo_activity")) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }
        composable(Screen.EventOverview.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            // Inject ViewModel and bind to EventManagementScreen
        }
        composable(Screen.Map.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            MapScreen(
                eventId = eventId,
                onNavigateToMessages = { navController.navigate(Screen.Chat.createRoute(eventId)) }
            )
        }
        composable(Screen.QuickDial.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            // Bind QuickDialScreen
        }
    }
}
```

---

## 4. AGENT EXECUTION & VALIDATION REQUIREMENTS

When you execute this refactoring, you must verify the following criteria:

1. **Zero Class Collisions:** Verify there are no imports or references to `entities.Activity`. Use `FieldEvent` everywhere.
2. **Complete User Data in Signup:** Ensure user registration captures `firstName`, `lastName`, `email`, and `phoneNumber` and writes them to the `users/{userId}` Firestore document.
3. **No Callback Hell:** Verify that `com.arielfaridja.ezrahi.entities.Callback` is deleted and replaced with `Flow` or `suspend fun`.
4. **No Factory Singletons:** Ensure `DataRepoFactory` is deleted; everything must be injected via `@Inject` using Hilt.
5. **No Dropped Screens:** `AuthScreen`, `EventManagementScreen`, `MapScreen`, `QuickDialScreen`, and `ChatScreen` must all be fully implemented and connected in the Navigation graph.
6. **Compile & Build Check:** Run `./gradlew assembleDebug` and verify that the build succeeds with **0 errors and 0 deprecation warnings**.