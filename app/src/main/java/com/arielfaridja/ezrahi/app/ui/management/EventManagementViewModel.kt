package com.arielfaridja.ezrahi.app.ui.management

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.model.EntityLivenessState
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.DeletionResolution
import com.arielfaridja.ezrahi.domain.model.MessengerOption
import com.arielfaridja.ezrahi.domain.model.ReportTypeDefinition
import com.arielfaridja.ezrahi.domain.model.RoleOption
import com.arielfaridja.ezrahi.domain.model.RouteInfo
import com.arielfaridja.ezrahi.domain.model.UserRole
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventManagementUiState(
    val event: FieldEvent? = null,
    val participants: List<EventParticipant> = emptyList(),
    val reports: List<FieldReport> = emptyList(),
    val routes: List<RouteInfo> = emptyList(),
    val roleOptions: List<RoleOption> = defaultRoleOptions(),
    val messengerOptions: List<MessengerOption> = emptyList(),
    val isManager: Boolean = false,
    val canManageRoutes: Boolean = false,
    val reportTypes: List<ReportTypeDefinition> = emptyList(),
    val deletionPreference: DeletionResolution? = null,
    val isUploading: Boolean = false,
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth,
    private val logger: ExceptionLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventManagementUiState())
    val uiState: StateFlow<EventManagementUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadEvent(eventId: String) {
        loadJob?.cancel()
        _uiState.update { EventManagementUiState(isLoading = true) }
        loadJob = viewModelScope.launch {
            repository.getEventUpdates(eventId).collect { event ->
                _uiState.update {
                    it.copy(
                        event = event,
                        isManager = event != null && event.managerId == auth.currentUser?.uid,
                        isLoading = false
                    )
                }
                refreshCanManageRoutes(eventId)
            }
        }
        viewModelScope.launch {
            repository.getParticipants(eventId).collect { list ->
                _uiState.update { it.copy(participants = list) }
                refreshCanManageRoutes(eventId)
            }
        }
        viewModelScope.launch {
            repository.getReports(eventId).collect { list ->
                _uiState.update { it.copy(reports = list) }
            }
        }
        viewModelScope.launch {
            repository.getRoutes(eventId).collect { routes ->
                _uiState.update { it.copy(routes = routes) }
            }
        }
        viewModelScope.launch {
            repository.getRoleOptions().collect { options ->
                _uiState.update { it.copy(roleOptions = options) }
            }
        }
        viewModelScope.launch {
            repository.getMessengerOptions().collect { options ->
                _uiState.update { it.copy(messengerOptions = options) }
            }
        }
        viewModelScope.launch {
            repository.getReportTypes(eventId).collect { types ->
                _uiState.update { it.copy(reportTypes = types) }
            }
        }
        viewModelScope.launch {
            repository.getDeletionPreference(eventId).collect { pref ->
                _uiState.update { it.copy(deletionPreference = pref) }
            }
        }
    }

    private fun refreshCanManageRoutes(eventId: String) {
        val state = _uiState.value
        val uid = auth.currentUser?.uid ?: return
        val event = state.event ?: return
        if (state.isManager) {
            _uiState.update { it.copy(canManageRoutes = true) }
            return
        }
        val myRole = state.participants.firstOrNull { it.userId == uid }?.role?.name
        val permitted = uid in event.routeAllowedUids ||
            (myRole != null && myRole in event.routeAllowedRoles)
        _uiState.update { it.copy(canManageRoutes = permitted) }
    }

    fun uploadRoute(eventId: String, uri: Uri, fileName: String) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isUploading = true) }
        viewModelScope.launch {
            repository.uploadRoute(eventId, uid, uri, fileName)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Route uploaded") }
                }
                .onFailure { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Upload failed: ${e.message}") }
                }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun setActiveRoute(eventId: String, routeId: String) {
        viewModelScope.launch {
            repository.setActiveRoute(eventId, routeId)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Route activated") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Activation failed: ${e.message}") }
                }
        }
    }

    fun deleteRoute(eventId: String, routeId: String) {
        viewModelScope.launch {
            val wasActive = _uiState.value.routes.firstOrNull { it.id == routeId }?.isActive == true
            repository.deleteRoute(eventId, routeId)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Route deleted") }
                    if (wasActive) {
                        _uiState.value.routes.firstOrNull { r -> r.id != routeId }?.let { next ->
                            repository.setActiveRoute(eventId, next.id)
                        }
                    }
                }
                .onFailure { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Delete failed: ${e.message}") }
                }
        }
    }

    fun updateRoutePermissions(eventId: String, allowedRoles: List<String>, allowedUids: List<String>) {
        viewModelScope.launch {
            repository.updateRoutePermissions(eventId, allowedRoles, allowedUids)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Permissions saved") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Permissions failed: ${e.message}") }
                }
        }
    }

    fun assignRole(eventId: String, userId: String, role: UserRole) {
        viewModelScope.launch {
            repository.updateParticipantRole(eventId, userId, role)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Role updated") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") }
                }
        }
    }

    fun renameEvent(eventId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateEventName(eventId, name)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Event renamed") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") }
                }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun updateStalenessConfig(eventId: String, config: StalenessConfig) {
        viewModelScope.launch {
            repository.updateStalenessConfig(eventId, config)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Staleness settings saved") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") }
                }
        }
    }

    fun addReportType(eventId: String, name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty() || trimmed.length > 32) {
                _uiState.update { it.copy(statusMessage = "Type name must be 1–32 characters") }
                return@launch
            }
            if (_uiState.value.reportTypes.any { it.name.equals(trimmed, ignoreCase = true) }) {
                _uiState.update { it.copy(statusMessage = "A type with this name already exists") }
                return@launch
            }
            repository.addReportType(eventId, trimmed, iconKey, colorHex)
                .onSuccess { _uiState.update { s -> s.copy(statusMessage = "Report type added") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed to add type: ${e.message}") }
                }
        }
    }

    fun updateReportType(eventId: String, typeId: String, name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty() || trimmed.length > 32) {
                _uiState.update { it.copy(statusMessage = "Type name must be 1–32 characters") }
                return@launch
            }
            if (_uiState.value.reportTypes.any { it.id != typeId && it.name.equals(trimmed, ignoreCase = true) }) {
                _uiState.update { it.copy(statusMessage = "A type with this name already exists") }
                return@launch
            }
            repository.updateReportType(eventId, typeId, trimmed, iconKey, colorHex)
                .onSuccess { _uiState.update { s -> s.copy(statusMessage = "Report type updated") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed to update type: ${e.message}") }
                }
        }
    }

    fun deleteReportType(eventId: String, typeId: String, resolution: DeletionResolution?) {
        viewModelScope.launch {
            repository.deleteReportType(eventId, typeId, resolution)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Report type deleted") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Delete failed: ${e.message}") }
                }
        }
    }

    fun setDeletionPreference(eventId: String, resolution: DeletionResolution?) {
        viewModelScope.launch {
            repository.setDeletionPreference(eventId, resolution)
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed to save preference: ${e.message}") }
                }
        }
    }

    fun updateParticipantManualState(eventId: String, userId: String, override: EntityLivenessState?) {
        viewModelScope.launch {
            val config = _uiState.value.event?.stalenessConfig ?: StalenessConfig()
            repository.updateParticipantManualState(eventId, userId, override, config)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Participant state updated") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.CAUGHT, eventId, screen = "management")
                    _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") }
                }
        }
    }
}