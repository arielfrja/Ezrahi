package com.arielfaridja.ezrahi.app.ui.dial

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDialScreen(
    eventId: String?,
    onOpenDrawer: () -> Unit,
    viewModel: QuickDialViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) {
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Dial") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        when {
            eventId == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select an activity first / בחר פעילות קודם")
                }
            }

            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val staff = state.participants
                    .filter { it.role != UserRole.MEMBER && it.phoneNumber.isNotBlank() }
                    .groupBy { it.role }
                    .toSortedMap(compareBy { staffOrder(it) })
                if (staff.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No staff contacts yet / אין אנשי צוות זמינים")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        staff.forEach { (role, members) ->
                            item(key = "header-$role") {
                                Text(
                                    text = roleLabel(role),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(members, key = { it.userId }) { member ->
                                StaffContactCard(
                                    participant = member,
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${member.phoneNumber}")
                                        )
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffContactCard(participant: EventParticipant, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.fullName.ifBlank { participant.userId },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = participant.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Phone,
                contentDescription = "Call ${participant.fullName}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun staffOrder(role: UserRole): Int = when (role) {
    UserRole.MANAGER -> 0
    UserRole.LEAD_GUIDE -> 1
    UserRole.MEDIC -> 2
    UserRole.SWEEP_GUIDE -> 3
    UserRole.LOGISTICS -> 4
    UserRole.MEMBER -> 5
}

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.MANAGER -> "Manager / מנהל פעילות"
    UserRole.LEAD_GUIDE -> "Lead Guide / מוביל"
    UserRole.MEDIC -> "Medic / חובש"
    UserRole.SWEEP_GUIDE -> "Sweep Guide / מאסף"
    UserRole.LOGISTICS -> "Logistics / לוגיסטיקה"
    UserRole.MEMBER -> "Participant / משתתף"
}