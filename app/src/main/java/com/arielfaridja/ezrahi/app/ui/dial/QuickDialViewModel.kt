package com.arielfaridja.ezrahi.app.ui.dial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickDialUiState(
    val participants: List<EventParticipant> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class QuickDialViewModel @Inject constructor(
    private val repository: EzrahiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickDialUiState())
    val uiState: StateFlow<QuickDialUiState> = _uiState.asStateFlow()

    private var participantsJob: Job? = null

    fun loadEvent(eventId: String) {
        participantsJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        participantsJob = viewModelScope.launch {
            repository.getParticipants(eventId).collect { list ->
                _uiState.update { it.copy(participants = list, isLoading = false) }
            }
        }
    }
}