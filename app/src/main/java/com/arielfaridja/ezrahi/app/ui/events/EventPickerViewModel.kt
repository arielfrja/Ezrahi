package com.arielfaridja.ezrahi.app.ui.events

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.app.util.EventPrefs
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventPickerUiState(
    val events: List<FieldEvent> = emptyList(),
    val isLoading: Boolean = true,
    val lastEventId: String? = null
)

@HiltViewModel
class EventPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventPickerUiState())
    val uiState: StateFlow<EventPickerUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(lastEventId = EventPrefs.getLastEventId(context))
        }
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            repository.getUserEvents(userId).collect { events ->
                _uiState.update { it.copy(events = events, isLoading = false) }
            }
        }
    }
}