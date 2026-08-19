package com.arielfaridja.ezrahi.app.ui.management

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.app.util.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.MessengerOption
import com.arielfaridja.ezrahi.domain.model.RoleOption
import com.arielfaridja.ezrahi.domain.model.RouteInfo
import com.arielfaridja.ezrahi.domain.model.UserRole
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
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
    val isUploading: Boolean = false,
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
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
                    if (_uiState.value.routes.none { r -> r.isActive }) {
                        _uiState.value.routes.firstOrNull()?.let { first ->
                            repository.setActiveRoute(eventId, first.id)
                                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Uploaded, but activation failed: ${e.message}") } }
                        }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Upload failed: ${e.message}") } }
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun setActiveRoute(eventId: String, routeId: String) {
        viewModelScope.launch {
            repository.setActiveRoute(eventId, routeId)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Route activated") } }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Activation failed: ${e.message}") } }
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
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Delete failed: ${e.message}") } }
        }
    }

    fun updateRoutePermissions(eventId: String, allowedRoles: List<String>, allowedUids: List<String>) {
        viewModelScope.launch {
            repository.updateRoutePermissions(eventId, allowedRoles, allowedUids)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Permissions saved") } }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Permissions failed: ${e.message}") } }
        }
    }

    fun assignRole(eventId: String, userId: String, role: UserRole) {
        viewModelScope.launch {
            repository.updateParticipantRole(eventId, userId, role)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Role updated") } }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") } }
        }
    }

    fun renameEvent(eventId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateEventName(eventId, name)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Event renamed") } }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") } }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}