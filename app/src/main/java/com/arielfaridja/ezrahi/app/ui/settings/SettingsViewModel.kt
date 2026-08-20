package com.arielfaridja.ezrahi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.MessengerOption
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

data class SettingsUiState(
    val messengerOptions: List<MessengerOption> = emptyList(),
    val myMessengers: Map<String, String> = emptyMap(),
    val myName: String = "",
    val myPhone: String = "",
    val myRole: UserRole? = null,
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth,
    private val logger: ExceptionLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadEvent(eventId: String) {
        loadJob?.cancel()
        _uiState.update { SettingsUiState(isLoading = true) }
        val uid = auth.currentUser?.uid ?: return
        loadJob = viewModelScope.launch {
            repository.getMyMessengers(eventId, uid).collect { messengers ->
                _uiState.update { it.copy(myMessengers = messengers, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getMessengerOptions().collect { options ->
                _uiState.update { it.copy(messengerOptions = options) }
            }
        }
        viewModelScope.launch {
            repository.getParticipants(eventId).collect { participants ->
                val me = participants.firstOrNull { it.userId == uid }
                _uiState.update {
                    it.copy(
                        myName = me?.fullName ?: "",
                        myPhone = me?.phoneNumber ?: "",
                        myRole = me?.role
                    )
                }
            }
        }
    }

    fun saveMessengers(eventId: String, messengers: Map<String, String>) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.updateMyMessengers(eventId, uid, messengers)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Saved") } }
                .onFailure { e ->
                    logger.log(e, ErrorType.NETWORK, eventId, screen = "settings")
                    _uiState.update { it.copy(statusMessage = "Failed: ${e.message}") }
                }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}