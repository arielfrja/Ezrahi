package com.arielfaridja.ezrahi.app.ui.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.core.network.transport.TacticalDispatchEngine
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.ReportTypeDefinition
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.arielfaridja.ezrahi.location.AdaptiveLocationEngine
import com.arielfaridja.ezrahi.location.GpsFix
import com.arielfaridja.ezrahi.location.LocationProfileData
import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val event: FieldEvent? = null,
    val participants: List<EventParticipant> = emptyList(),
    val reports: List<FieldReport> = emptyList(),
    val reportTypes: List<ReportTypeDefinition> = emptyList(),
    val routePoints: List<GeoPoint> = emptyList(),
    val activeRouteName: String? = null,
    val isSosActive: Boolean = false,
    val statusMessage: String? = null
)

data class HudState(
    val fix: GpsFix? = null,
    val strategyLabel: String = "--",
    val batteryPercent: Int? = null,
    val pendingOutbox: Int = 0,
    val online: Boolean = true,
    val lowPowerActive: Boolean = false
)

private fun strategyName(config: LocationProfileData): String =
    when (config.profile.name) {
        "STATIONARY" -> if (config.isLowPowerActive) "Still·LP" else "Still"
        "FOOT_PATROL" -> if (config.isLowPowerActive) "Foot·LP" else "Foot"
        "VEHICLE" -> if (config.isLowPowerActive) "Veh·LP" else "Veh"
        else -> config.profile.name
    }

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth,
    private val logger: ExceptionLogger,
    private val adaptiveEngine: AdaptiveLocationEngine,
    private val dispatchEngine: TacticalDispatchEngine,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _pendingOutbox = MutableStateFlow(0)
    private val _online = MutableStateFlow(true)
    private val _reportTypes = MutableStateFlow<List<ReportTypeDefinition>>(emptyList())
    val reportTypes: StateFlow<List<ReportTypeDefinition>> = _reportTypes.asStateFlow()

    val hudState: StateFlow<HudState> = combine(
        adaptiveEngine.lastFix,
        adaptiveEngine.effectiveConfig,
        adaptiveEngine.batteryPercent,
        _pendingOutbox,
        _online
    ) { fix, config, battery, outbox, online ->
        HudState(
            fix = fix,
            strategyLabel = if (adaptiveEngine.isEmergencySos.value) "SOS" else strategyName(config),
            batteryPercent = battery,
            pendingOutbox = outbox,
            online = online,
            lowPowerActive = config.isLowPowerActive
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HudState())

    init {
        viewModelScope.launch {
            while (isActive) {
                _pendingOutbox.value = runCatching { dispatchEngine.pendingCount() }.getOrDefault(_pendingOutbox.value)
                _online.value = isNetworkOnline()
                delay(5000)
            }
        }
    }

    private fun isNetworkOnline(): Boolean = runCatching {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

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
        viewModelScope.launch {
            repository.getReports(eventId).collect { list ->
                _uiState.update { it.copy(reports = list) }
            }
        }
        viewModelScope.launch {
            repository.getRoutes(eventId).collect { routes ->
                val name = if (routes.size == 1) routes.first().name
                else routes.firstOrNull { r -> r.isActive }?.name
                _uiState.update { it.copy(activeRouteName = name) }
            }
        }
        viewModelScope.launch {
            repository.getActiveRoutePoints(eventId).collect { points ->
                _uiState.update { it.copy(routePoints = points) }
            }
        }
        viewModelScope.launch {
            repository.getReportTypes(eventId).collect { types ->
                _reportTypes.value = types
                _uiState.update { it.copy(reportTypes = types) }
            }
        }
        viewModelScope.launch {
            repository.routeErrorEvents.collect { error ->
                _uiState.update { it.copy(statusMessage = error) }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun logServiceStartFailure(e: Exception) {
        logger.log(e, ErrorType.LOCATION_SERVICE, screen = "map")
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
            } else {
                result.exceptionOrNull()?.let { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "map")
                    _uiState.update { it.copy(statusMessage = "SOS failed: ${e.message}") }
                }
            }
        }
    }

    fun addReport(eventId: String, title: String, description: String, type: FieldReportType, lat: Double, lng: Double, typeId: String? = null) {
        viewModelScope.launch {
            val user = auth.currentUser
            val report = FieldReport(
                actId = eventId,
                reporterId = user?.uid ?: "anonymous",
                title = title,
                description = description,
                location = GeoPoint(lat, lng, System.currentTimeMillis()),
                type = type,
                typeId = typeId
            )
            repository.addReport(report)
                .onFailure { e -> logger.log(e, ErrorType.CAUGHT, eventId, screen = "map") }
        }
    }

    fun addReport(eventId: String, title: String, description: String, typeDef: ReportTypeDefinition, lat: Double, lng: Double) {
        val legacy = when {
            !typeDef.builtin -> FieldReportType.UNKNOWN
            typeDef.name.equals("MEDICAL", ignoreCase = true) -> FieldReportType.MEDICAL
            else -> FieldReportType.GENERAL
        }
        addReport(eventId, title, description, legacy, lat, lng, typeDef.id)
    }

    fun pingParticipant(eventId: String, targetUserId: String, targetName: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val result = repository.sendDirectMessage(
                eventId = eventId,
                myUserId = user.uid,
                myName = user.email ?: "Staff",
                otherUserId = targetUserId,
                text = "📍 PING — $targetName, please acknowledge your position."
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Ping sent to $targetName") }
            } else {
                result.exceptionOrNull()?.let { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "map")
                    _uiState.update { it.copy(statusMessage = "Ping failed: ${e.message}") }
                }
            }
        }
    }
}
