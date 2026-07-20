package io.surprise.ciphertun.compose.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.surprise.ciphertun.R
import io.surprise.ciphertun.compose.base.UiEvent
import io.surprise.ciphertun.compose.navigation.NewProfileArgs
import io.surprise.ciphertun.compose.topbar.OverrideTopBar
import io.surprise.ciphertun.constant.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onOpenNewProfile: (NewProfileArgs) -> Unit = {},
    onToggleConnection: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    OverrideTopBar {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }

    LaunchedEffect(serviceStatus) {
        viewModel.updateServiceStatus(serviceStatus)
    }

    if (uiState.showDeprecatedDialog && uiState.deprecatedNotes.isNotEmpty()) {
        val note = uiState.deprecatedNotes.first()
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.error_deprecated_warning)) },
            text = { Text(note.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeprecatedNote() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton =
            if (!note.migrationLink.isNullOrBlank()) {
                {
                    TextButton(onClick = {
                        viewModel.sendGlobalEvent(UiEvent.OpenUrl(note.migrationLink))
                        viewModel.dismissDeprecatedNote()
                    }) {
                        Text(stringResource(R.string.error_deprecated_documentation))
                    }
                }
            } else {
                null
            },
        )
    }

    if (uiState.showProfilePickerSheet) {
        ProfilePickerSheet(
            profiles = uiState.profiles,
            selectedProfileId = uiState.selectedProfileId,
            onProfileSelected = { profile -> viewModel.selectProfile(profile.id) },
            onProfileEdit = viewModel::editProfile,
            onProfileDelete = viewModel::deleteProfile,
            onProfileMove = viewModel::moveProfile,
            onDismiss = viewModel::hideProfilePickerSheet,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        ConnectButton(status = uiState.serviceStatus, onClick = onToggleConnection)

        Text(
            text = statusLabel(uiState.serviceStatus),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        ActiveConfigCard(
            configName = uiState.selectedProfileName,
            downlinkTotal = uiState.downlinkTotal,
            uplinkTotal = uiState.uplinkTotal,
            onClick = { viewModel.showProfilePickerSheet() },
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Composable
private fun ConnectButton(status: Status, onClick: () -> Unit) {
    val ringColor = when (status) {
        Status.Started -> MaterialTheme.colorScheme.secondary
        Status.Starting, Status.Stopping -> MaterialTheme.colorScheme.tertiary
        Status.Stopped -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(top = 32.dp)
            .size(200.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, ringColor, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            Icons.Filled.PowerSettingsNew,
            contentDescription = "Toggle connection",
            tint = ringColor,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
        )
    }
}

private fun statusLabel(status: Status): String = when (status) {
    Status.Started -> "CONNECTED"
    Status.Starting -> "CONNECTING"
    Status.Stopping -> "DISCONNECTING"
    Status.Stopped -> "NOT CONNECTED"
}

@Composable
private fun ActiveConfigCard(
    configName: String?,
    downlinkTotal: String,
    uplinkTotal: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = "Active Configuration",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = configName ?: "Not Set",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                TrafficStat(
                    label = "Downlink",
                    value = downlinkTotal,
                    icon = Icons.Filled.KeyboardArrowDown,
                    color = MaterialTheme.colorScheme.primary,
                )
                TrafficStat(
                    label = "Uplink",
                    value = uplinkTotal,
                    icon = Icons.Filled.KeyboardArrowUp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun TrafficStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
