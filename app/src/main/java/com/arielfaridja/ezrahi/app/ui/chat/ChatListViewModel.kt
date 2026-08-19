package com.arielfaridja.ezrahi.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.app.util.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.RoleOption
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val messages: List<FieldMessage> = emptyList(),
    val roleOptions: List<RoleOption> = defaultRoleOptions(),
    val participants: List<EventParticipant> = emptyList(),
    val dmPreviews: Map<String, String> = emptyMap(),
    val myUid: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
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
            repository.getParticipants(eventId).collect { list ->
                val others = list.filter { p -> p.userId != uid }
                _uiState.update {
                    it.copy(
                        participants = others,
                        myUid = uid
                    )
                }
                if (others.isNotEmpty()) {
                    others.forEach { other ->
                        val preview = repository.getDirectLastMessage(eventId, uid, other.userId)
                        _uiState.update { state ->
                            state.copy(dmPreviews = state.dmPreviews + (other.userId to (preview ?: "")))
                        }
                    }
                }
            }
        }
    }
}