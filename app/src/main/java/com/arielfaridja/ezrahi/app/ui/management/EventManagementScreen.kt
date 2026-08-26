package com.arielfaridja.ezrahi.app.ui.management

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import com.arielfaridja.ezrahi.domain.model.EntityLivenessState
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.domain.model.roleLabel
import com.arielfaridja.ezrahi.app.ui.reports.ReportIconCatalog
import com.arielfaridja.ezrahi.domain.model.DeletionResolution
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.MessengerOption
import com.arielfaridja.ezrahi.domain.model.RoleOption
import com.arielfaridja.ezrahi.domain.model.ReportTypeDefinition
import com.arielfaridja.ezrahi.domain.model.RouteInfo
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
    var overrideTarget by remember { mutableStateOf<EventParticipant?>(null) }
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
                            if (state.canManageRoutes) {
                                item(key = "routes-section") {
                                    RoutesSection(
                                        routes = state.routes,
                                        isUploading = state.isUploading,
                                        isManager = state.isManager,
                                        roleOptions = state.roleOptions,
                                        participants = state.participants,
                                        allowedRoles = state.event?.routeAllowedRoles ?: emptyList(),
                                        allowedUids = state.event?.routeAllowedUids ?: emptyList(),
                                        onUpload = { uri, name -> viewModel.uploadRoute(eventId, uri, name) },
                                        onActivate = { routeId -> viewModel.setActiveRoute(eventId, routeId) },
                                        onDelete = { routeId -> viewModel.deleteRoute(eventId, routeId) },
                                        onSavePermissions = { roles, uids ->
                                            viewModel.updateRoutePermissions(eventId, roles, uids)
                                        }
                                    )
                                }
                            }
                            if (state.isManager) {
                                item(key = "report-types-preference") {
                                    DeletionPreferenceSection(
                                        current = state.deletionPreference,
                                        onChange = { viewModel.setDeletionPreference(eventId, it) }
                                    )
                                }
                                item(key = "staleness-settings") {
                                    StalenessSettingsSection(
                                        config = state.event?.stalenessConfig ?: StalenessConfig(),
                                        onSave = { viewModel.updateStalenessConfig(eventId, it) }
                                    )
                                }
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
                                    stalenessConfig = state.event?.stalenessConfig ?: StalenessConfig(),
                                    messengerOptions = state.messengerOptions,
                                    onEditRequest = { editingParticipant = participant },
                                    onOverrideRequest = { overrideTarget = participant }
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
                            if (state.isManager && REPORT_TYPES_EDITOR_ENABLED) {
                                item(key = "manage-types") {
                                    var showTypeSheet by remember { mutableStateOf(false) }
                                    OutlinedButton(onClick = { showTypeSheet = true }) {
                                        Text("Manage report types")
                                    }
                                    if (showTypeSheet) {
                                        ReportTypesSheet(
                                            types = state.reportTypes,
                                            reports = state.reports,
                                            rememberedPreference = state.deletionPreference,
                                            onDismiss = { showTypeSheet = false },
                                            onAdd = { name, icon, color -> viewModel.addReportType(eventId, name, icon, color) },
                                            onUpdate = { id, name, icon, color -> viewModel.updateReportType(eventId, id, name, icon, color) },
                                            onDelete = { type, resolution, remember ->
                                                viewModel.deleteReportType(eventId, type.id, resolution)
                                                viewModel.setDeletionPreference(eventId, if (remember) resolution else null)
                                            }
                                        )
                                    }
                                }
                            }
                            item(key = "reports-summary") {
                                ReportsSummary(reports = state.reports, types = state.reportTypes)
                            }
                            items(state.reports, key = { "r-${it.id}" }) { report ->
                                ReportRow(report = report, types = state.reportTypes)
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
    overrideTarget?.let { participant ->
        val currentEventId = eventId ?: return@let
        ManualStateDialog(
            participant = participant,
            onConfirm = { stateOverride ->
                viewModel.updateParticipantManualState(currentEventId, participant.userId, stateOverride)
                overrideTarget = null
            },
            onDismiss = { overrideTarget = null }
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
    stalenessConfig: StalenessConfig,
    messengerOptions: List<MessengerOption>,
    onEditRequest: () -> Unit,
    onOverrideRequest: () -> Unit
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
                    val liveState = participant.effectiveState(stalenessConfig)
                    Text(
                        text = when (liveState) {
                            EntityLivenessState.ACTIVE -> "Active"
                            EntityLivenessState.STALE -> "Stale"
                            EntityLivenessState.DISCONNECTED -> "Disconnected"
                            EntityLivenessState.EXPIRED -> "Faded"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (liveState) {
                            EntityLivenessState.ACTIVE -> Color(0xFF2E7D32)
                            EntityLivenessState.STALE -> Color(0xFFF9A825)
                            EntityLivenessState.DISCONNECTED -> Color(0xFF616161)
                            EntityLivenessState.EXPIRED -> Color(0xFF9E9E9E)
                        }
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
                    TextButton(onClick = onOverrideRequest) {
                        Text("State")
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
private fun StalenessSettingsSection(
    config: StalenessConfig,
    onSave: (StalenessConfig) -> Unit
) {
    var stale by remember { mutableStateOf(config.staleThresholdMinutes.toString()) }
    var disconnected by remember { mutableStateOf(config.disconnectedThresholdMinutes.toString()) }
    var expired by remember { mutableStateOf(config.expiredThresholdMinutes.toString()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Staleness Thresholds / ספי רעננות",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Minutes before a participant is marked (Amber / Grey / Hidden)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            StalenessNumberField("Stale (Amber)", stale) { stale = it }
            StalenessNumberField("Disconnected (Grey)", disconnected) { disconnected = it }
            StalenessNumberField("Expired / Faded (Hidden)", expired) { expired = it }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val s = stale.toIntOrNull() ?: config.staleThresholdMinutes
                    val d = disconnected.toIntOrNull() ?: config.disconnectedThresholdMinutes
                    val e = expired.toIntOrNull() ?: config.expiredThresholdMinutes
                    onSave(StalenessConfig(s, d, e))
                }
            ) {
                Text("Save thresholds")
            }
        }
    }
}

@Composable
private fun StalenessNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualStateDialog(
    participant: EventParticipant,
    onConfirm: (EntityLivenessState?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<EntityLivenessState?>(participant.manualStateOverride) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set state override") },
        text = {
            Column {
                Text(
                    text = participant.fullName.ifBlank { participant.userId },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                EntityLivenessState.entries.forEach { state ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selected == state,
                            onClick = { selected = state }
                        )
                        Text(stateLabel(state))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selected == null,
                        onClick = { selected = null }
                    )
                    Text("Clear override (auto)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun stateLabel(state: EntityLivenessState): String = when (state) {
    EntityLivenessState.ACTIVE -> "Active (Green)"
    EntityLivenessState.STALE -> "Stale (Amber)"
    EntityLivenessState.DISCONNECTED -> "Disconnected (Grey)"
    EntityLivenessState.EXPIRED -> "Faded / Hidden"
}

private fun parseTypeColor(hex: String?, fallback: Color = Color(0xFF757575)): Color =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: fallback

@Composable
private fun ReportsSummary(reports: List<FieldReport>, types: List<ReportTypeDefinition>) {
    val typesById = types.associateBy { it.id }
    val counts = reports.groupingBy { report ->
        report.typeId?.let { id -> typesById[id]?.name } ?: when (report.type) {
            FieldReportType.MEDICAL -> "MEDICAL"
            FieldReportType.GENERAL -> "GENERAL"
            FieldReportType.UNKNOWN -> "Other"
        }
    }.eachCount()
    val ordered = counts.entries.sortedByDescending { it.value }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Total: ${reports.size}")
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ordered.take(4).forEach { (label, count) ->
                    val def = types.firstOrNull { it.name.equals(label, ignoreCase = true) }
                    val accent = def?.let { parseTypeColor(it.colorHex, ReportIconCatalog.entry(it.iconKey).accent) }
                        ?: Color(0xFF757575)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(ReportIconCatalog.entry(def?.iconKey ?: "general").resId),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("$label: $count", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (ordered.size > 4) {
                Text(
                    "+ ${ordered.drop(4).joinToString { "${it.key}: ${it.value}" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun typeLabelFor(report: FieldReport, types: List<ReportTypeDefinition>): String =
    types.firstOrNull { it.id == report.typeId }?.name ?: when (report.type) {
        FieldReportType.MEDICAL -> "MEDICAL"
        FieldReportType.GENERAL -> "GENERAL"
        FieldReportType.UNKNOWN -> "Custom"
    }

@Composable
private fun ReportRow(report: FieldReport, types: List<ReportTypeDefinition>) {
    val label = typeLabelFor(report, types)
    val def = types.firstOrNull { it.id == report.typeId }
    val entry = def?.let { ReportIconCatalog.entry(it.iconKey) }
    val accent = def?.let { parseTypeColor(it.colorHex, entry?.accent ?: Color(0xFF757575)) }
        ?: if (report.type == FieldReportType.MEDICAL) MaterialTheme.colorScheme.error else Color(0xFF757575)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry != null) {
                    Icon(
                        painter = painterResource(entry.resId),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = report.title.ifBlank { "Untitled report" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
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

// ---------------------------------------------------------------------------
// Dynamic report types (spec docs/specs/dynamic-report-types.md §9)
// ---------------------------------------------------------------------------

/** Gates the type editor until the new Firestore rules are deployed (§11). */
private const val REPORT_TYPES_EDITOR_ENABLED = true

@Composable
private fun DeletionPreferenceSection(current: DeletionResolution?, onChange: (DeletionResolution?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Report type deletion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (current) {
                    is DeletionResolution.RemoveReports -> "Remembered: remove affected reports"
                    is DeletionResolution.ConvertToGeneral -> "Remembered: convert to General"
                    is DeletionResolution.ConvertTo -> "Remembered: convert to chosen type"
                    null -> "Ask every time a type is deleted"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showDialog = true }) { Text("Change") }
        }
    }
    if (showDialog) {
        DeletionPreferenceDialog(
            current = current,
            onConfirm = { resolution ->
                onChange(resolution)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun DeletionPreferenceDialog(
    current: DeletionResolution?,
    onConfirm: (DeletionResolution?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<Int>(current.toOptionIndex()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When deleting a report type") },
        text = {
            Column {
                listOf(
                    "Ask every time" to null,
                    "Remove affected reports" to DeletionResolution.RemoveReports,
                    "Convert them to General" to DeletionResolution.ConvertToGeneral
                ).forEachIndexed { index, (label, resolution) ->
                    val optionIndex = if (resolution == null) 0 else index
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == optionIndex, onClick = { selected = optionIndex })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    when (selected) {
                        1 -> DeletionResolution.RemoveReports
                        2 -> DeletionResolution.ConvertToGeneral
                        else -> null
                    }
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun DeletionResolution?.toOptionIndex(): Int = when (this) {
    is DeletionResolution.RemoveReports -> 1
    is DeletionResolution.ConvertToGeneral -> 2
    else -> 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportTypesSheet(
    types: List<ReportTypeDefinition>,
    reports: List<FieldReport>,
    rememberedPreference: DeletionResolution?,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
    onUpdate: (String, String, String, String) -> Unit,
    onDelete: (ReportTypeDefinition, DeletionResolution, Boolean) -> Unit
) {
    var editingType by remember { mutableStateOf<ReportTypeDefinition?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletingType by remember { mutableStateOf<ReportTypeDefinition?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Report types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    editingType = null
                    showEditor = true
                }) { Text("+ Add") }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(types, key = { it.id }) { def ->
                    val entry = ReportIconCatalog.entry(def.iconKey)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(entry.resId),
                            contentDescription = null,
                            tint = parseTypeColor(def.colorHex, entry.accent)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.name)
                            if (def.builtin) {
                                Text(
                                    "Built-in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            editingType = def
                            showEditor = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${def.name}")
                        }
                        if (!def.builtin) {
                            IconButton(onClick = { deletingType = def }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${def.name}")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showEditor) {
        ReportTypeEditorDialog(
            existing = editingType,
            takenNames = types.map { it.name },
            onConfirm = { name, icon, colorHex ->
                val target = editingType
                if (target == null) onAdd(name, icon, colorHex) else onUpdate(target.id, name, icon, colorHex)
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    deletingType?.let { target ->
        val affectedCount = reports.count { it.typeId == target.id }
        val remainingTypes = types.filter { it.id != target.id }
        // Remembered preference applies automatically without prompting (§5.1),
        // unless its conversion target no longer exists.
        val remembered = rememberedPreference?.takeIf { pref ->
            pref !is DeletionResolution.ConvertTo || remainingTypes.any { it.id == pref.targetTypeId }
        }
        if (remembered != null) {
            onDelete(target, remembered, false)
            deletingType = null
        } else {
            DeletionResolutionDialog(
                typeName = target.name,
                affectedCount = affectedCount,
                remainingTypes = remainingTypes,
                onConfirm = { resolution, rememberChoice ->
                    onDelete(target, resolution, rememberChoice)
                    deletingType = null
                },
                onDismiss = { deletingType = null }
            )
        }
    }
}

private val TYPE_COLOR_PALETTE = listOf(
    "#2E7D32", "#C62828", "#F9A825", "#E64A19", "#1565C0", "#00796B",
    "#795548", "#EF6C00", "#3949AB", "#AD1457", "#5E35B1", "#455A64"
)

private fun colorToHex(c: Color): String = String.format("#%06X", 0xFFFFFF and c.toArgb())

@Composable
private fun ReportTypeEditorDialog(
    existing: ReportTypeDefinition?,
    takenNames: List<String>,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember(existing) { mutableStateOf(existing?.iconKey ?: "general") }
    var colorHex by remember(existing) {
        mutableStateOf(existing?.colorHex ?: colorToHex(ReportIconCatalog.entry(existing?.iconKey ?: "general").accent))
    }
    val duplicate = takenNames.any { it.equals(name.trim(), ignoreCase = true) && !it.equals(existing?.name, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add report type" else "Edit report type") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = duplicate || name.length > 32,
                    supportingText = {
                        when {
                            duplicate -> Text("Name already in use")
                            name.length > 32 -> Text("Maximum 32 characters")
                            else -> Text("${name.trim().length}/32")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                IconPickerGrid(selected = iconKey, onSelect = { iconKey = it })
                Spacer(Modifier.height(12.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TYPE_COLOR_PALETTE.forEach { hex ->
                        val selected = hex.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseTypeColor(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    painter = painterResource(ReportIconCatalog.entry(iconKey).resId),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, iconKey, colorHex) },
                enabled = name.isNotBlank() && name.trim().length <= 32 && !duplicate
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun IconPickerGrid(selected: String, onSelect: (String) -> Unit) {
    val gridKeys = remember { ReportIconCatalog.entries.keys.toList() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
    ) {
        gridItems(gridKeys) { key ->
            val entry = ReportIconCatalog.entry(key)
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                leadingIcon = {
                    Icon(painterResource(entry.resId), contentDescription = key, tint = entry.accent)
                },
                label = { Text("") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletionResolutionDialog(
    typeName: String,
    affectedCount: Int,
    remainingTypes: List<ReportTypeDefinition>,
    onConfirm: (DeletionResolution, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var option by remember { mutableIntStateOf(0) }   // 0=convert general 1=convert other 2=remove all
    var targetType by remember { mutableStateOf<ReportTypeDefinition?>(remainingTypes.firstOrNull()) }
    var rememberChoice by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"$typeName\"?") },
        text = {
            Column {
                Text(
                    "$affectedCount report(s) use this type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                RadioButtonRow(
                    selected = option == 0,
                    onClick = { option = 0 },
                    label = "Convert them to General"
                )
                RadioButtonRow(
                    selected = option == 1,
                    onClick = { option = 1 },
                    label = "Convert them to another type"
                )
                var menuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = menuExpanded && option == 1,
                    onExpandedChange = { if (option == 1) menuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = targetType?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = option == 1,
                        label = { Text("Target type") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded && option == 1,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        remainingTypes.forEach { def ->
                            DropdownMenuItem(
                                text = {
                                    val entry = ReportIconCatalog.entry(def.iconKey)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(entry.resId),
                                            contentDescription = null,
                                            tint = parseTypeColor(def.colorHex, entry.accent),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(def.name)
                                    }
                                },
                                onClick = {
                                    targetType = def
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
                RadioButtonRow(
                    selected = option == 2,
                    onClick = { option = 2 },
                    label = "Remove them all"
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                    Text("Remember my choice")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resolution = when (option) {
                        1 -> DeletionResolution.ConvertTo(targetType?.id ?: return@TextButton)
                        2 -> DeletionResolution.RemoveReports
                        else -> DeletionResolution.ConvertToGeneral
                    }
                    onConfirm(resolution, rememberChoice)
                }
            ) { Text("Delete type") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RadioButtonRow(selected: Boolean, onClick: () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()

@Composable
private fun RoutesSection(
    routes: List<RouteInfo>,
    isUploading: Boolean,
    isManager: Boolean,
    roleOptions: List<RoleOption>,
    participants: List<EventParticipant>,
    allowedRoles: List<String>,
    allowedUids: List<String>,
    onUpload: (Uri, String) -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSavePermissions: (List<String>, List<String>) -> Unit
) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "route.gpx"
            onUpload(uri, name)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Routes / מסלולים",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (routes.isEmpty()) {
                Text(
                    text = "No routes yet — upload the planned trail (GPX).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            } else {
                routes.forEach { route ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (route.isActive) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (route.isActive) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            TextButton(onClick = { onActivate(route.id) }) {
                                Text("Activate")
                            }
                        }
                        IconButton(onClick = { onDelete(route.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete route ${route.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Button(
                onClick = { filePicker.launch("*/*") },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isUploading) "Uploading..." else "Upload Route / העלה מסלול")
            }

            if (isManager) {
                Spacer(Modifier.height(16.dp))
                RoutePermissionsEditor(
                    roleOptions = roleOptions,
                    participants = participants,
                    allowedRoles = allowedRoles,
                    allowedUids = allowedUids,
                    onSave = onSavePermissions
                )
            }
        }
    }
}

@Composable
private fun RoutePermissionsEditor(
    roleOptions: List<RoleOption>,
    participants: List<EventParticipant>,
    allowedRoles: List<String>,
    allowedUids: List<String>,
    onSave: (List<String>, List<String>) -> Unit
) {
    var selectedRoles by remember { mutableStateOf(allowedRoles) }
    var selectedUids by remember { mutableStateOf(allowedUids) }

    Text(
        text = "Who can upload routes / מי רשאי להעלות מסלולים",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Roles",
        style = MaterialTheme.typography.bodySmall
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        roleOptions.forEach { option ->
            val selected = option.name in selectedRoles
            FilterChip(
                selected = selected,
                onClick = {
                    selectedRoles = if (selected) selectedRoles - option.name else selectedRoles + option.name
                },
                label = { Text(option.label) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Members",
        style = MaterialTheme.typography.bodySmall
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        participants.forEach { participant ->
            val selected = participant.userId in selectedUids
            FilterChip(
                selected = selected,
                onClick = {
                    selectedUids = if (selected) selectedUids - participant.userId else selectedUids + participant.userId
                },
                label = { Text(participant.fullName.ifBlank { participant.userId }) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { onSave(selectedRoles, selectedUids) },
        enabled = selectedRoles != allowedRoles || selectedUids != allowedUids
    ) {
        Text("Save permissions")
    }
}