package com.arielfaridja.ezrahi.app.ui.direct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectThreadUiState(
    val messages: List<FieldMessage> = emptyList(),
    val otherName: String = "",
    val myName: String = "",
    val statusMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DirectThreadViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectThreadUiState())
    val uiState: StateFlow<DirectThreadUiState> = _uiState.asStateFlow()

    fun loadThread(eventId: String, otherUserId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getDirectMessages(eventId, uid, otherUserId).collect { list ->
                _uiState.update {
                    it.copy(messages = list, isLoading = false)
                }
            }
        }
        viewModelScope.launch {
            repository.getParticipants(eventId).collect { participants ->
                val me = participants.firstOrNull { it.userId == uid }
                val other = participants.firstOrNull { it.userId == otherUserId }
                _uiState.update {
                    it.copy(
                        myName = me?.fullName ?: "",
                        otherName = other?.fullName ?: otherUserId
                    )
                }
            }
        }
    }

    fun send(eventId: String, otherUserId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.sendDirectMessage(
                eventId = eventId,
                myUserId = uid,
                myName = _uiState.value.myName.ifBlank { auth.currentUser?.email ?: "Anonymous" },
                otherUserId = otherUserId,
                text = trimmed
            )
            if (result.isFailure) {
                _uiState.update {
                    it.copy(statusMessage = result.exceptionOrNull()?.message ?: "Failed to send message")
                }
            }
        }
    }
}