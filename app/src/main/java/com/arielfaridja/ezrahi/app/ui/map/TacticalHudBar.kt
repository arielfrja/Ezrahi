package com.arielfaridja.ezrahi.app.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed

data class HudDisplay(
    val coordinateText: String,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val online: Boolean,
    val pendingOutbox: Int,
    val batteryPercent: Int?,
    val strategyLabel: String,
    val lowPowerActive: Boolean
)

@Composable
fun TacticalHudBar(
    hud: HudDisplay,
    onCycleCoordinateFormat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            HudChip(
                icon = Icons.Default.GpsFixed,
                primary = hud.coordinateText,
                secondary = gpsQuality(hud),
                onClick = onCycleCoordinateFormat,
                modifier = Modifier.weight(1.4f)
            )
            HudChip(
                icon = if (hud.online) Icons.Default.CloudDone else Icons.Default.CloudOff,
                primary = if (hud.online) "Firebase" else "Offline",
                secondary = "Outbox: ${hud.pendingOutbox}",
                modifier = Modifier.weight(1f)
            )
            HudChip(
                icon = if (hud.lowPowerActive) Icons.Default.Bolt else Icons.Default.Battery5Bar,
                primary = batteryText(hud.batteryPercent),
                secondary = hud.strategyLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun gpsQuality(hud: HudDisplay): String {
    val acc = hud.accuracyMeters?.let { "±${it.toInt()}m" } ?: "--"
    val alt = hud.altitudeMeters?.let { "${it.toInt()}m" } ?: "--"
    return "$acc · Alt $alt"
}

private fun batteryText(percent: Int?): String =
    percent?.let { "$it%" } ?: "--%"

@Composable
private fun HudChip(
    icon: ImageVector,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            ) else Modifier
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 1.dp).align(Alignment.CenterHorizontally)
        )
        Text(
            text = primary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = secondary,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
