package com.arielfaridja.ezrahi.app.ui.management

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.app.util.roleLabel
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.MessengerOption
import com.arielfaridja.ezrahi.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    eventId: String?,
    onOpenDrawer: () -> Unit,
    viewModel: EventManagementViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) {
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var roleFilter by remember { mutableStateOf<UserRole?>(null) }
    var editingParticipant by remember { mutableStateOf<EventParticipant?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val roleOptions = state.roleOptions.mapNotNull { option ->
        runCatching { UserRole.valueOf(option.name) }.getOrNull()
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Event Management") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Details") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Crew") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Reports") }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item(key = "event-card") {
                                EventCard(
                                    eventName = state.event?.name ?: "",
                                    managerId = state.event?.managerId ?: "",
                                    isManager = state.isManager,
                                    onRename = { name -> viewModel.renameEvent(eventId, name) }
                                )
                            }
                        }

                        1 -> {
                            item(key = "participants-header") {
                                Text(
                                    text = "Crew Members / משתתפים",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item(key = "participants-filter") {
                                RoleFilterRow(
                                    selected = roleFilter,
                                    onSelect = { roleFilter = it },
                                    roles = roleOptions
                                )
                            }
                            val filtered = state.participants.filter {
                                roleFilter == null || it.role == roleFilter
                            }
                            items(filtered, key = { "p-${it.userId}" }) { participant ->
                                ParticipantRow(
                                    participant = participant,
                                    canEditRoles = state.isManager,
                                    messengerOptions = state.messengerOptions,
                                    onEditRequest = { editingParticipant = participant }
                                )
                            }
                            if (filtered.isEmpty()) {
                                item(key = "participants-empty") {
                                    Text(
                                        text = "No participants match this filter / אין משתתפים מתאימים",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        else -> {
                            item(key = "reports-header") {
                                Text(
                                    text = "Field Reports / דיווחים מהשטח",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item(key = "reports-summary") {
                                ReportsSummary(reports = state.reports)
                            }
                            items(state.reports, key = { "r-${it.id}" }) { report ->
                                ReportRow(report = report)
                            }
                        }
                    }
                }
            }
        }
    }

    editingParticipant?.let { participant ->
        val currentEventId = eventId ?: return@let
        RoleEditDialog(
            participant = participant,
            availableRoles = roleOptions,
            onConfirm = { role ->
                viewModel.assignRole(currentEventId, participant.userId, role)
                editingParticipant = null
            },
            onDismiss = { editingParticipant = null }
        )
    }
}

@Composable
private fun EventCard(
    eventName: String,
    managerId: String,
    isManager: Boolean,
    onRename: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = eventName.ifBlank { "Unnamed event" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manager: $managerId",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (isManager) {
                var newName by remember { mutableStateOf(eventName) }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Rename event") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onRename(newName) },
                    enabled = newName.isNotBlank() && newName != eventName
                ) {
                    Text("Save name")
                }
            } else {
                Text(
                    text = "You are a participant — only the manager can edit this event.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoleFilterRow(
    selected: UserRole?,
    onSelect: (UserRole?) -> Unit,
    roles: List<UserRole>
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        roles.forEach { role ->
            FilterChip(
                selected = selected == role,
                onClick = { onSelect(role) },
                label = { Text(roleLabel(role)) }
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: EventParticipant,
    canEditRoles: Boolean,
    messengerOptions: List<MessengerOption>,
    onEditRequest: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participant.fullName.ifBlank { participant.userId },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = participant.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = roleLabel(participant.role),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canEditRoles) {
                    IconButton(onClick = onEditRequest) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change role of ${participant.fullName}"
                        )
                    }
                }
            }
            val available = messengerOptions.filter { option ->
                participant.messengers[option.id]?.isNotBlank() == true
            }
            if (available.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    available.forEach { option ->
                        val handle = participant.messengers[option.id] ?: ""
                        TextButton(
                            onClick = {
                                val url = option.urlTemplate.replace("{handle}", handle)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            }
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleEditDialog(
    participant: EventParticipant,
    availableRoles: List<UserRole>,
    onConfirm: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(participant.role) }
    var menuExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change role") },
        text = {
            Column {
                Text(
                    text = participant.fullName.ifBlank { participant.userId },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = roleLabel(selectedRole),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        availableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(roleLabel(role)) },
                                onClick = {
                                    selectedRole = role
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedRole != participant.role) onConfirm(selectedRole)
                    else onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReportsSummary(reports: List<FieldReport>) {
    val medicalCount = reports.count { it.type == FieldReportType.MEDICAL }
    val generalCount = reports.count { it.type == FieldReportType.GENERAL }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Total: ${reports.size}")
            Text("Medical: $medicalCount", color = MaterialTheme.colorScheme.error)
            Text("General: $generalCount")
        }
    }
}

@Composable
private fun ReportRow(report: FieldReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.title.ifBlank { "Untitled report" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = reportTypeLabel(report.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (report.type == FieldReportType.MEDICAL) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                text = "By ${report.reporterId} · ${report.reportTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (report.description.isNotBlank()) {
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun reportTypeLabel(type: FieldReportType): String = when (type) {
    FieldReportType.MEDICAL -> "MEDICAL"
    FieldReportType.GENERAL -> "GENERAL"
    FieldReportType.UNKNOWN -> "UNKNOWN"
}