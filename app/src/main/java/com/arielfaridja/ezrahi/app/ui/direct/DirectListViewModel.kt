package com.arielfaridja.ezrahi.app.ui.direct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectListUiState(
    val participants: List<EventParticipant> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DirectListViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectListUiState())
    val uiState: StateFlow<DirectListUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: ""
            repository.getParticipants(eventId).collect { list ->
                _uiState.update {
                    it.copy(
                        participants = list.filter { p -> p.userId != uid },
                        isLoading = false
                    )
                }
            }
        }
    }
}