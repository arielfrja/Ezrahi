package com.arielfaridja.ezrahi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.model.FieldMessage
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

data class ChatUiState(
    val messages: List<FieldMessage> = emptyList(),
    val roleOptions: List<RoleOption> = defaultRoleOptions(),
    val myRole: UserRole? = null,
    val myName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    fun loadEvent(eventId: String) {
        messagesJob?.cancel()
        _uiState.update { ChatUiState() }
        messagesJob = viewModelScope.launch {
            repository.getMessages(eventId).collect { list ->
                _uiState.update {
                    it.copy(
                        messages = list.sortedBy { m -> m.timestamp },
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getRoleOptions().collect { options ->
                _uiState.update { it.copy(roleOptions = options) }
            }
        }
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: ""
            repository.getParticipants(eventId).collect { participants ->
                val me = participants.firstOrNull { it.userId == uid }
                _uiState.update {
                    it.copy(
                        myRole = me?.role,
                        myName = me?.fullName ?: ""
                    )
                }
            }
        }
    }

    fun sendMessage(eventId: String, text: String, targetRole: RoleOption?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val senderRole = _uiState.value.myRole ?: UserRole.MANAGER
            repository.sendMessage(
                FieldMessage(
                    eventId = eventId,
                    senderId = user.uid,
                    senderName = _uiState.value.myName.ifBlank { user.email ?: "Anonymous" },
                    senderRole = senderRole,
                    targetRole = targetRole?.let {
                        runCatching { UserRole.valueOf(it.name) }.getOrNull()
                    },
                    messageText = trimmed
                )
            )
        }
    }
}