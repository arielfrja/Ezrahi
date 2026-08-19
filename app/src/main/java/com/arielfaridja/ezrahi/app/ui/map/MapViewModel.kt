package com.arielfaridja.ezrahi.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldEvent
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val event: FieldEvent? = null,
    val participants: List<EventParticipant> = emptyList(),
    val reports: List<FieldReport> = emptyList(),
    val isSosActive: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: EzrahiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            repository.getEventUpdates(eventId).collect { event ->
                _uiState.update { it.copy(event = event) }
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
    }

    fun triggerSOS(eventId: String, currentLat: Double, currentLng: Double) {
        viewModelScope.launch {
            val user = auth.currentUser
            val result = repository.sendSOS(
                eventId = eventId,
                senderId = user?.uid ?: "anonymous",
                senderName = user?.email ?: "Staff Member",
                location = GeoPoint(currentLat, currentLng)
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isSosActive = true, statusMessage = "🚨 SOS Transmitted!") }
            }
        }
    }

    fun addReport(eventId: String, title: String, description: String, type: FieldReportType, lat: Double, lng: Double) {
        viewModelScope.launch {
            val user = auth.currentUser
            val report = FieldReport(
                actId = eventId,
                reporterId = user?.uid ?: "anonymous",
                title = title,
                description = description,
                location = GeoPoint(lat, lng, System.currentTimeMillis()),
                type = type
            )
            repository.addReport(report)
        }
    }
}
