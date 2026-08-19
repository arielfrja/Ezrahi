package com.arielfaridja.ezrahi.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.RoleOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    eventId: String,
    onOpenDrawer: () -> Unit,
    onNewDirect: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDirect) {
                Icon(Icons.Default.Add, contentDescription = "New direct message")
            }
        }
    ) { padding ->
        when {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "header-channels") {
                        Text(
                            text = "Channels / ערוצים",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    item(key = "channel-all") {
                        ConversationRow(
                            title = "All Broadcast",
                            subtitle = lastPreview(
                                state.messages.filter { it.targetRole == null || it.isEmergency }
                            ),
                            icon = { Icon(Icons.Default.Email, contentDescription = null) },
                            onClick = { onOpenChannel("ALL") }
                        )
                    }
                    items(state.roleOptions, key = { "channel-${it.name}" }) { option ->
                        ConversationRow(
                            title = option.label,
                            subtitle = lastPreview(
                                state.messages.filter { it.targetRole?.name == option.name }
                            ),
                            icon = { Icon(Icons.Default.Email, contentDescription = null) },
                            onClick = { onOpenChannel(option.name) }
                        )
                    }
                    item(key = "header-direct") {
                        Text(
                            text = "Direct / אישי",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    items(state.participants, key = { "dm-${it.userId}" }) { participant ->
                        ConversationRow(
                            title = participant.fullName.ifBlank { participant.userId },
                            subtitle = state.dmPreviews[participant.userId]
                                ?.takeIf { it.isNotBlank() }
                                ?.let { preview ->
                                    if (preview.length > 60) preview.take(60) + "..." else preview
                                }
                                ?: "No messages yet",
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = { onOpenThread(participant.userId) }
                        )
                    }
                    if (state.participants.isEmpty()) {
                        item(key = "direct-empty") {
                            Text(
                                text = "No other participants yet / אין משתתפים נוספים",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun lastPreview(messages: List<FieldMessage>): String =
    messages.maxByOrNull { it.timestamp }?.messageText?.let { preview ->
        if (preview.length > 60) preview.take(60) + "..." else preview
    } ?: ""