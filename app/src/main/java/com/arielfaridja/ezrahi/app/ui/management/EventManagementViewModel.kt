package com.arielfaridja.ezrahi.app.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.app.util.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.MessengerOption
import com.arielfaridja.ezrahi.domain.model.RoleOption
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
    val roleOptions: List<RoleOption> = defaultRoleOptions(),
    val messengerOptions: List<MessengerOption> = emptyList(),
    val isManager: Boolean = false,
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