# 🗺️ Ezrahi Step-by-Step Modernization Roadmap
### A Field Guide for Junior & Intermediate Android Developers

---

## 🎯 How to Use This Roadmap
This roadmap is engineered specifically as a **playbook**. You do not need to be a senior architect or a compiler wizard to execute this. Every phase is broken into bite-sized, sequential micro-tasks with explicit file paths, exact code samples, and **"Watch Out!"** callouts for common pitfalls.

Work through the phases in order. Do not skip ahead, as each phase builds directly on the clean foundation established by the previous one.

```
       PROJECT MODERNIZATION PIPELINE (CHRONOLOGICAL ORDER)
  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
  │   Phase 0    │ ──► │   Phase 1    │ ──► │   Phase 2    │ ──► │   Phase 3    │
  │ Gradle & KTS │     │ Domain Clean │     │  Dagger Hilt │     │ Room Offline │
  └──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                        │
  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
  │   Phase 7    │ ◄── │   Phase 6    │ ◄── │  Phase 4 & 5 │ ◄──────────┘
  │ LoRa / Mesh  │     │ AppSec Rules │     │ Compose & Loc│
  └──────────────┘     └──────────────┘     └──────────────┘
```

---

## 📦 Phase 0: Project Setup & Gradle Modernization (Days 1–3)
**Objective:** Replace legacy Groovy Gradle with Kotlin DSL (`.gradle.kts`), configure a modern Version Catalog (`libs.versions.toml`), and set up modern Android Tooling.

---

### Task 0.1: Git Safety Check & Branching
1. Create a clean branch from your current working code:
   ```bash
   git checkout -b refactor/modernization-v2
   git push -u origin refactor/modernization-v2
   ```
2. Keep your original `main` or `master` branch untouched as a fallback reference.

---

### Task 0.2: Create the Version Catalog
Create a file at `gradle/libs.versions.toml`. This centralizes all dependencies and prevents version conflicts.

📁 **File: `gradle/libs.versions.toml`**
```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.0"
ksp = "2.0.0-1.0.24"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.4"
activityCompose = "1.9.1"
composeBom = "2024.08.00"
navigationCompose = "2.7.7"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
coroutines = "1.8.1"
firebaseBom = "33.1.2"
playServicesLocation = "21.3.0"
mapsCompose = "6.1.0"
playServicesMaps = "19.0.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }

# Compose
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Hilt (Dependency Injection)
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Room (Local Database)
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-storage = { group = "com.google.firebase", name = "firebase-storage-ktx" }

# Maps & Location
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
play-services-maps = { group = "com.google.android.gms", name = "play-services-maps", version.ref = "playServicesMaps" }
maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

---

### Task 0.3: Convert Gradle Files to Kotlin DSL (`.gradle.kts`)

📁 **File: `build.gradle.kts` (Root Project Level)**
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google-services) apply false
}
```

📁 **File: `app/build.gradle.kts` (App Module Level)**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google-services)
}

android {
    namespace = "com.arielfaridja.ezrahi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arielfaridja.ezrahi"
        minSdk = 26 // Android 8.0 Oreo: supports robust notifications and background services
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play-services)

    // Firebase (BoM manages versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Maps & Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
}
```

> 💡 **Beginner Tip:** After editing these files, click **"Sync Now"** in Android Studio. Ensure there are zero sync errors before writing code.

---

## 🏛️ Phase 1: Domain Entities & Cleaning Technical Debt (Days 4–7)
**Objective:** Fix the naming collision where `entities.Activity` conflicts with `android.app.Activity`, convert all legacy Java models into clean Kotlin `data class` files, and delete the custom `Callback` interface.

---

### Task 1.1: Rename `Activity` to `FieldEvent`
Delete the old `com.arielfaridja.ezrahi.entities.Activity.java` and create the new domain models in a clean package structure.

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/domain/model/FieldModels.kt`**
```kotlin
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
```

---

### Task 1.2: Delete Legacy `Callback.java` and `DataRepoFactory.java`
* Delete `com/arielfaridja/ezrahi/entities/Callback.java`.
* Instead of callbacks, all repository calls will use Kotlin's built-in `suspend fun` (returns a single value or `Result<T>`) or `Flow<T>` (emits real-time streams of data).

---

## 💉 Phase 2: Dependency Injection with Dagger Hilt (Days 8–11)
**Objective:** Replace manual `DataRepoFactory.getInstance()` calls with automated, clean Hilt Dependency Injection.

---

### Task 2.1: Create the Application Class
Create a new Kotlin class inheriting from `android.app.Application`.

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/EzrahiApp.kt`**
```kotlin
package com.arielfaridja.ezrahi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EzrahiApp : Application()
```

Register it in your manifest:

📁 **File: `app/src/main/AndroidManifest.xml`**
```xml
<application
    android:name=".EzrahiApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@android:style/Theme.Material.Light.NoActionBar">
    <!-- activities will go here -->
</application>
```

---

### Task 2.2: Setup Firebase Hilt Module
Provide single instances of Firebase Auth, Firestore, and Storage.

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/di/FirebaseModule.kt`**
```kotlin
package com.arielfaridja.ezrahi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}
```

---

## 💾 Phase 3: Offline-First Data Layer with Room (Days 12–17)
**Objective:** Build an offline database so that if guides enter a valley with no reception, the app retains all cached participants, messages, and map routes without crashing.

---

### Task 3.1: Room Database Entities & DAOs

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/data/local/LocalEntities.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arielfaridja.ezrahi.domain.model.UserRole

@Entity(tableName = "cached_events")
data class EventLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val managerId: String,
    val managerContact: String,
    val gpxRouteUrl: String?,
    val isLive: Boolean
)

@Entity(tableName = "cached_participants")
data class ParticipantLocalEntity(
    @PrimaryKey val userId: String,
    val eventId: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String,
    val latitude: Double,
    val longitude: Double,
    val isOnline: Boolean,
    val lastSeenTimestamp: Long
)

@Entity(tableName = "cached_messages")
data class MessageLocalEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val targetRole: String?,
    val messageText: String,
    val isEmergency: Boolean,
    val timestamp: Long
)
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/data/local/EzrahiDao.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EzrahiDao {

    // Events
    @Query("SELECT * FROM cached_events WHERE id = :eventId")
    fun observeEvent(eventId: String): Flow<EventLocalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventLocalEntity)

    // Participants
    @Query("SELECT * FROM cached_participants WHERE eventId = :eventId")
    fun observeParticipants(eventId: String): Flow<List<ParticipantLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantLocalEntity>)

    // Messages
    @Query("SELECT * FROM cached_messages WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun observeMessages(eventId: String): Flow<List<MessageLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageLocalEntity)
}
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/data/local/EzrahiDatabase.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventLocalEntity::class, ParticipantLocalEntity::class, MessageLocalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EzrahiDatabase : RoomDatabase() {
    abstract fun ezrahiDao(): EzrahiDao
}
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/di/DatabaseModule.kt`**
```kotlin
package com.arielfaridja.ezrahi.di

import android.content.Context
import androidx.room.Room
import com.arielfaridja.ezrahi.data.local.EzrahiDao
import com.arielfaridja.ezrahi.data.local.EzrahiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EzrahiDatabase {
        return Room.databaseBuilder(
            context,
            EzrahiDatabase::class.java,
            "ezrahi_local_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideEzrahiDao(db: EzrahiDatabase): EzrahiDao = db.ezrahiDao()
}
```

---

### Task 3.2: Create the Offline-First Repository
This repository reads from Room (for instant offline UI rendering) and listens to Firebase Firestore updates in real time to sync changes into Room.

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/domain/repository/EzrahiRepository.kt`**
```kotlin
package com.arielfaridja.ezrahi.domain.repository

import com.arielfaridja.ezrahi.domain.model.*
import kotlinx.coroutines.flow.Flow

interface EzrahiRepository {
    fun getEventUpdates(eventId: String): Flow<FieldEvent?>
    fun getParticipants(eventId: String): Flow<List<EventParticipant>>
    fun getMessages(eventId: String): Flow<List<FieldMessage>>
    suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit>
    suspend fun sendMessage(message: FieldMessage): Result<Unit>
    suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit>
}
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/data/repository/EzrahiRepositoryImpl.kt`**
```kotlin
package com.arielfaridja.ezrahi.data.repository

import com.arielfaridja.ezrahi.data.local.*
import com.arielfaridja.ezrahi.domain.model.*
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EzrahiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dao: EzrahiDao
) : EzrahiRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getEventUpdates(eventId: String): Flow<FieldEvent?> {
        // 1. Listen to Firestore and cache locally
        firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val event = EventLocalEntity(
                        id = snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        managerId = snapshot.getString("managerId") ?: "",
                        managerContact = snapshot.getString("managerContact") ?: "",
                        gpxRouteUrl = snapshot.getString("gpxRouteUrl"),
                        isLive = snapshot.getBoolean("isLive") ?: true
                    )
                    scope.launch { dao.insertEvent(event) }
                }
            }

        // 2. Emit from local Room database (Offline-First)
        return dao.observeEvent(eventId).map { local ->
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
    }

    override fun getParticipants(eventId: String): Flow<List<EventParticipant>> {
        firestore.collection("events").document(eventId).collection("participants")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        ParticipantLocalEntity(
                            userId = doc.id,
                            eventId = eventId,
                            fullName = doc.getString("fullName") ?: "",
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            role = doc.getString("role") ?: UserRole.MEMBER.name,
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            isOnline = doc.getBoolean("isOnline") ?: true,
                            lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                        )
                    }
                    scope.launch { dao.insertParticipants(list) }
                }
            }

        return dao.observeParticipants(eventId).map { list ->
            list.map {
                EventParticipant(
                    userId = it.userId,
                    fullName = it.fullName,
                    phoneNumber = it.phoneNumber,
                    role = runCatching { UserRole.valueOf(it.role) }.getOrDefault(UserRole.MEMBER),
                    currentLocation = GeoPoint(it.latitude, it.longitude, it.lastSeenTimestamp),
                    isOnline = it.isOnline,
                    lastSeenTimestamp = it.lastSeenTimestamp
                )
            }
        }
    }

    override fun getMessages(eventId: String): Flow<List<FieldMessage>> {
        firestore.collection("events").document(eventId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        val doc = change.document
                        val msg = MessageLocalEntity(
                            id = doc.id,
                            eventId = eventId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderRole = doc.getString("senderRole") ?: UserRole.MEMBER.name,
                            targetRole = doc.getString("targetRole"),
                            messageText = doc.getString("messageText") ?: "",
                            isEmergency = doc.getBoolean("isEmergency") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                        scope.launch { dao.insertMessage(msg) }
                    }
                }
            }

        return dao.observeMessages(eventId).map { list ->
            list.map {
                FieldMessage(
                    id = it.id,
                    eventId = it.eventId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderRole = runCatching { UserRole.valueOf(it.senderRole) }.getOrDefault(UserRole.MEMBER),
                    targetRole = it.targetRole?.let { roleStr -> runCatching { UserRole.valueOf(roleStr) }.getOrNull() },
                    messageText = it.messageText,
                    isEmergency = it.isEmergency,
                    timestamp = it.timestamp
                )
            }
        }
    }

    override suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit> = runCatching {
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastSeenTimestamp" to location.timestamp
        )
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .update(data).await()
    }

    override suspend fun sendMessage(message: FieldMessage): Result<Unit> = runCatching {
        firestore.collection("events").document(message.eventId)
            .collection("messages").document(message.id.ifEmpty { firestore.collection("events").document().id })
            .set(message).await()
    }

    override suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit> = runCatching {
        val sosMessage = FieldMessage(
            id = "SOS_${System.currentTimeMillis()}",
            eventId = eventId,
            senderId = senderId,
            senderName = senderName,
            senderRole = UserRole.MEMBER,
            targetRole = null,
            messageText = "🚨 EMERGENCY SOS! I need immediate assistance at (${location.latitude}, ${location.longitude})",
            isEmergency = true,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(sosMessage).getOrThrow()
    }
}
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/di/RepositoryModule.kt`**
```kotlin
package com.arielfaridja.ezrahi.di

import com.arielfaridja.ezrahi.data.repository.EzrahiRepositoryImpl
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEzrahiRepository(
        impl: EzrahiRepositoryImpl
    ): EzrahiRepository
}
```

---

## 📍 Phase 4: Permissions & Background Location Service (Days 18–22)
**Objective:** Transmit GPS coordinates in the background while guides hike, even when the phone screen is off or the user is looking at another app.

---

### Task 4.1: Declare Permissions & Foreground Service in Manifest

📁 **File: `app/src/main/AndroidManifest.xml`**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Location Permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    
    <!-- Service & Network Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".EzrahiApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <!-- Single Host Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Location Foreground Service -->
        <service
            android:name=".service.LocationTrackingService"
            android:foregroundServiceType="location"
            android:exported="false" />

    </application>
</manifest>
```

---

### Task 4.2: Build the Foreground Location Tracking Service

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/service/LocationTrackingService.kt`**
```kotlin
package com.arielfaridja.ezrahi.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.arielfaridja.ezrahi.MainActivity
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var repository: EzrahiRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var eventId: String = ""
    private var userId: String = ""

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    if (eventId.isNotEmpty() && userId.isNotEmpty()) {
                        serviceScope.launch {
                            repository.updateLocation(
                                eventId = eventId,
                                userId = userId,
                                location = GeoPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        eventId = intent?.getStringExtra("EXTRA_EVENT_ID") ?: ""
        userId = intent?.getStringExtra("EXTRA_USER_ID") ?: ""

        val notification = createNotification()
        startForeground(1001, notification)
        startLocationUpdates()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L) // every 10 seconds
            .setMinUpdateIntervalMillis(5000L)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "ezrahi_tracking_channel")
            .setContentTitle("Ezrahi Field Tracking Active")
            .setContentText("Transmitting your location to field staff...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ezrahi_tracking_channel",
            "Field Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## 🎨 Phase 5: Single-Activity & Jetpack Compose UI Migration (Days 23–32)
**Objective:** Replace all separate Activities (`LoginActivity`, `SignupActivity`, `StartupActivity`) and XML Fragments with a single, ultra-fast Jetpack Compose application.

---

### Task 5.1: Material 3 Theme & Typography
Create a theme supporting dynamic colors, high-contrast outdoor visibility, and Hebrew RTL alignment.

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/ui/theme/Theme.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D8FF),
    secondary = Color(0xFFFFB74D),
    error = Color(0xFFFF5252),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006590),
    secondary = Color(0xFFE65100),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFBFDFE),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun EzrahiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
```

---

### Task 5.2: Create `MainActivity` & Compose Navigation

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/MainActivity.kt`**
```kotlin
package com.arielfaridja.ezrahi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arielfaridja.ezrahi.ui.auth.AuthScreen
import com.arielfaridja.ezrahi.ui.map.MapScreen
import com.arielfaridja.ezrahi.ui.theme.EzrahiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EzrahiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EzrahiNavApp()
                }
            }
        }
    }
}

@Composable
fun EzrahiNavApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(onAuthSuccess = {
                navController.navigate("map/demo_event_123") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("map/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            MapScreen(
                eventId = eventId,
                onNavigateToMessages = { navController.navigate("messages/$eventId") }
            )
        }
    }
}
```

---

### Task 5.3: Build Screen 1 – Authentication Composable (`AuthScreen.kt`)

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/ui/auth/AuthScreen.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ezrahi / אזרחי", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Field Command & Coordination", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email / כתובת אימייל") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password / סיסמה") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            isLoading = false
                            onAuthSuccess()
                        }
                        .addOnFailureListener {
                            // If user doesn't exist, create an account automatically for demo/testing
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    isLoading = false
                                    onAuthSuccess()
                                }
                                .addOnFailureListener { err ->
                                    isLoading = false
                                    errorMessage = err.localizedMessage
                                }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            } else {
                Text("Login / כניסה למערכת")
            }
        }
    }
}
```

---

### Task 5.4: Build Screen 2 – Interactive Real-Time Map with SOS Button (`MapScreen.kt`)

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/ui/map/MapViewModel.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val event: FieldEvent? = null,
    val participants: List<EventParticipant> = emptyList(),
    val isSosActive: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            repository.getEventUpdates(eventId).collect { event ->
                _uiState.update { it.copy(event = event) }
            }
        }
        viewModelScope.launch {
            repository.getParticipants(eventId).collect { list ->
                _uiState.update { it.copy(participants = list) }
            }
        }
    }

    fun triggerSOS(eventId: String, currentLat: Double, currentLng: Double) {
        viewModelScope.launch {
            val user = auth.currentUser
            val result = repository.sendSOS(
                eventId = eventId,
                senderId = user?.uid ?: "anonymous",
                senderName = user?.email ?: "Staff Member",
                location = GeoPoint(currentLat, currentLng)
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isSosActive = true, statusMessage = "🚨 SOS Transmitted!") }
            }
        }
    }
}
```

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/ui/map/MapScreen.kt`**
```kotlin
package com.arielfaridja.ezrahi.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    eventId: String,
    onNavigateToMessages: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val israelCenter = LatLng(31.7683, 35.2137) // Jerusalem default
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(israelCenter, 10f)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val target = cameraPositionState.position.target
                    viewModel.triggerSOS(eventId, target.latitude, target.longitude)
                },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS")
                    Spacer(Modifier.width(8.dp))
                    Text("SOS / מצוקה")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Render participant markers
                state.participants.forEach { participant ->
                    participant.currentLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                            title = "${participant.fullName} (${participant.role})",
                            snippet = "Last seen: ${participant.isOnline}"
                        )
                    }
                }
            }

            // Top Status Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.event?.name ?: "Field Activity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onNavigateToMessages) {
                        Text("Messages")
                    }
                }
            }
        }
    }
}
```

---

## 🔒 Phase 6: Cloud Security & Rules Hardening (Days 33–36)
**Objective:** Protect minor participants' real-time location data and restrict unauthorized database tampering using strict Firebase Cloud Security Rules.

---

### Task 6.1: Deploy Firestore Security Rules

📁 **File: `firestore.rules` (Root Directory)**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isSignedIn() {
      return request.auth != null;
    }

    function isEventParticipant(eventId) {
      return isSignedIn() &&
        exists(/databases/$(database)/documents/events/$(eventId)/participants/$(request.auth.uid));
    }

    // 1. Events collection
    match /events/{eventId} {
      // Anyone logged in can read an event or join
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update, delete: if isSignedIn() && resource.data.managerId == request.auth.uid;

      // 2. Participants sub-collection (Live Location Tracking)
      match /participants/{userId} {
        allow read: if isEventParticipant(eventId);
        // A user can only overwrite their OWN coordinates
        allow write: if isSignedIn() && request.auth.uid == userId;
      }

      // 3. Messages sub-collection
      match /messages/{messageId} {
        allow read: if isEventParticipant(eventId);
        allow create: if isEventParticipant(eventId) && request.resource.data.senderId == request.auth.uid;
      }
    }
  }
}
```

---

## 📻 Phase 7: LoRa / Mesh Architecture Preparation (Days 37–40)
**Objective:** Prepare an abstraction layer that allows the app to seamlessly switch to off-grid hardware (e.g., a **Meshtastic BLE device** or custom LoRa transceiver) when internet connectivity fails.

---

### Task 7.1: Build the Mesh Hardware Interface

📁 **File: `app/src/main/java/com/arielfaridja/ezrahi/domain/mesh/MeshTransceiver.kt`**
```kotlin
package com.arielfaridja.ezrahi.domain.mesh

import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface MeshTransceiver {
    fun isDeviceConnected(): Flow<Boolean>
    suspend fun connectToNode(bluetoothAddress: String): Result<Unit>
    suspend fun broadcastLocationPacket(location: GeoPoint): Result<Unit>
    suspend fun broadcastEmergencyPacket(message: FieldMessage): Result<Unit>
    fun observeIncomingPackets(): Flow<FieldMessage>
}
```

> 💡 **Why this matters:** When you add the Bluetooth LoRa module later, you only need to write a class that implements `MeshTransceiver`. Your ViewModels, Room database, and Map UI **do not need any rewriting**.

---

## 📋 Comprehensive Junior Developer Checklist

Print or copy this checklist into your GitHub Issues / Kanban board:

```markdown
- [ ] Phase 0: Project Setup & Gradle
  - [ ] 0.1 Create Git branch `refactor/modernization-v2`
  - [ ] 0.2 Add `gradle/libs.versions.toml` with catalog versions
  - [ ] 0.3 Convert `build.gradle` to `build.gradle.kts` (Root + App)
  - [ ] 0.4 Run Gradle Sync and ensure 0 errors

- [ ] Phase 1: Domain Entities & Cleanup
  - [ ] 1.1 Delete legacy `entities/Activity.java` (Rename to `FieldEvent.kt`)
  - [ ] 1.2 Convert all models to Kotlin data classes in `domain.model`
  - [ ] 1.3 Delete custom `Callback.java` and `DataRepoFactory.java`

- [ ] Phase 2: Dagger Hilt DI Setup
  - [ ] 2.1 Create `EzrahiApp.kt` annotated with `@HiltAndroidApp`
  - [ ] 2.2 Register application in `AndroidManifest.xml`
  - [ ] 2.3 Create `FirebaseModule.kt` (Provides Auth, Firestore, Storage)

- [ ] Phase 3: Room Database & Offline-First Repo
  - [ ] 3.1 Create Room entities (`EventLocalEntity`, `ParticipantLocalEntity`, `MessageLocalEntity`)
  - [ ] 3.2 Create `EzrahiDao.kt` with Flow observables
  - [ ] 3.3 Build `EzrahiRepositoryImpl.kt` syncing Firestore into Room
  - [ ] 3.4 Bind repository interface in `RepositoryModule.kt`

- [ ] Phase 4: Permissions & Background Location
  - [ ] 4.1 Add Fine/Coarse Location & Foreground Service permissions in Manifest
  - [ ] 4.2 Create `LocationTrackingService.kt` with persistent notification

- [ ] Phase 5: Single-Activity Compose UI
  - [ ] 5.1 Create `Theme.kt` with Material 3 palette
  - [ ] 5.2 Set up `MainActivity.kt` with `NavHost`
  - [ ] 5.3 Implement `AuthScreen.kt` (Login + Sign-up with Firebase)
  - [ ] 5.4 Implement `MapScreen.kt` with live markers and SOS Floating Action Button

- [ ] Phase 6: Cloud Security
  - [ ] 6.1 Update and deploy `firestore.rules` protecting locations & roles

- [ ] Phase 7: Off-Grid LoRa Prep
  - [ ] 7.1 Implement `MeshTransceiver.kt` interface for future Bluetooth/LoRa expansion
```