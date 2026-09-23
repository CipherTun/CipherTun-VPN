package io.surprise.ciphertun.compose.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.surprise.ciphertun.R
import io.surprise.ciphertun.compose.component.RemoteControlMenuItems
import io.surprise.ciphertun.compose.component.rememberRemoteServers
import io.surprise.ciphertun.compose.navigation.NewProfileArgs
import io.surprise.ciphertun.compose.screen.configs.PingResult
import io.surprise.ciphertun.compose.screen.configs.ProfilePingTester
import io.surprise.ciphertun.database.Profile
import io.surprise.ciphertun.compose.topbar.LocalScaffoldPadding
import io.surprise.ciphertun.compose.topbar.OverrideTopBar
import io.surprise.ciphertun.constant.Status
import io.surprise.ciphertun.utils.RemoteControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class CardRenderItem(val cards: List<CardGroup>, val isRow: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onOpenNewProfile: (NewProfileArgs) -> Unit = {},
    onToggleService: () -> Unit = {},
    onOpenConfigs: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteServer by RemoteControlManager.remoteServer.collectAsState()
    val remoteConnected by RemoteControlManager.isConnected.collectAsState()
    val isRemote = remoteServer != null
    val remoteServers by rememberRemoteServers()
    var showOthersMenu by remember { mutableStateOf(false) }

    OverrideTopBar {
        TopAppBar(
            title = { Text(stringResource(R.string.title_dashboard)) },
            actions = {
                Box {
                    IconButton(onClick = { showOthersMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.title_others),
                        )
                    }
                    DropdownMenu(
                        expanded = showOthersMenu,
                        onDismissRequest = { showOthersMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_items)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            onClick = {
                                showOthersMenu = false
                                viewModel.toggleCardSettingsDialog()
                            },
                        )
                        RemoteControlMenuItems(
                            servers = remoteServers,
                            onAction = { showOthersMenu = false },
                        )
                    }
                }
            },
        )
    }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Show dashboard settings bottom sheet
    if (uiState.showCardSettingsDialog) {
        DashboardSettingsBottomSheet(
            sheetState = sheetState,
            visibleCards = uiState.visibleCards,
            cardOrder = uiState.cardOrder,
            onToggleCard = viewModel::toggleCardVisibility,
            onReorderCards = viewModel::reorderCards,
            onResetOrder = viewModel::resetCardOrder,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    viewModel.closeCardSettingsDialog()
                }
            },
        )
    }

    if (isRemote && !remoteConnected) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val scaffoldPadding = LocalScaffoldPadding.current

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        val bottomPadding = when {
            showStartFab -> 88.dp
            showStatusBar -> 74.dp
            else -> 0.dp
        }
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            // Dynamic dashboard cards
            // Show cards when service is running OR if it's the Profiles card (always available)
            val serviceRunning = uiState.isStatusVisible

            // Filter cards based on availability
            val actuallyVisibleCards =
                uiState.visibleCards.filter { cardGroup ->
                    when {
                        // Profile management moved to the dedicated Configs
                        // tab — no longer shown as a Home card.
                        cardGroup == CardGroup.Profiles -> false

                        // The remote dashboard only renders cards backed by the
                        // command protocol: system proxy is an operation on
                        // the local device.
                        isRemote ->
                            cardGroup != CardGroup.SystemProxy &&
                                serviceRunning &&
                                isCardAvailableWhenServiceRunning(cardGroup, uiState)

                        else -> serviceRunning && isCardAvailableWhenServiceRunning(cardGroup, uiState)
                    }
                }.toSet()

            // Process cards to group half-width cards together
            val cardRenderItems =
                processCardsForRendering(
                    cardOrder = uiState.cardOrder,
                    visibleCards = actuallyVisibleCards,
                    cardWidths = uiState.cardWidths,
                )

            // CipherTun hero connect/disconnect button — local sessions only;
            // remote sessions use their own disconnect control in RemoteStatusBar.
            if (!isRemote) {
                item(key = "hero_connect_button") {
                    HeroConnectButton(
                        serviceStatus = serviceStatus,
                        onToggle = onToggleService,
                    )
                }

                item(key = "active_config_summary") {
                    ActiveConfigSummary(
                        profile = uiState.profiles.find { it.id == uiState.selectedProfileId },
                        onClick = onOpenConfigs,
                    )
                }
            }

            items(cardRenderItems) { renderItem ->
                if (renderItem.isRow && renderItem.cards.size >= 2) {
                    // Render two half-width cards in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        renderItem.cards.forEach { cardGroup ->
                            DashboardCardRenderer(
                                cardGroup = cardGroup,
                                cardWidth =
                                uiState.cardWidths[cardGroup]
                                    ?: CardWidth.Full,
                                uiState = uiState,
                                onClashModeSelected = viewModel::selectClashMode,
                                onSystemProxyToggle = viewModel::toggleSystemProxy,
                                // Profile card specific props
                                profiles = uiState.profiles,
                                selectedProfileId = uiState.selectedProfileId,
                                isLoading = uiState.isLoading,
                                showAddProfileSheet = uiState.showAddProfileSheet,
                                showProfilePickerSheet = uiState.showProfilePickerSheet,
                                updatingProfileId = uiState.updatingProfileId,
                                updatedProfileId = uiState.updatedProfileId,
                                onProfileSelected = viewModel::selectProfile,
                                onProfileEdit = viewModel::editProfile,
                                onProfileDelete = viewModel::deleteProfile,
                                onProfileShare = viewModel::shareProfile,
                                onProfileShareURL = viewModel::shareProfileURL,
                                onProfileUpdate = viewModel::updateProfile,
                                onProfileMove = viewModel::moveProfile,
                                onShowAddProfileSheet = viewModel::showAddProfileSheet,
                                onHideAddProfileSheet = viewModel::hideAddProfileSheet,
                                onShowProfilePickerSheet = viewModel::showProfilePickerSheet,
                                onHideProfilePickerSheet = viewModel::hideProfilePickerSheet,
                                onOpenNewProfile = onOpenNewProfile,
                                commandClient = viewModel.commandClient,
                                modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    // Render single card (full-width or single half-width)
                    renderItem.cards.forEach { cardGroup ->
                        DashboardCardRenderer(
                            cardGroup = cardGroup,
                            cardWidth =
                            uiState.cardWidths[cardGroup]
                                ?: CardWidth.Full,
                            uiState = uiState,
                            serviceStatus = serviceStatus,
                            onClashModeSelected = viewModel::selectClashMode,
                            onSystemProxyToggle = viewModel::toggleSystemProxy,
                            // Profile card specific props
                            profiles = uiState.profiles,
                            selectedProfileId = uiState.selectedProfileId,
                            isLoading = uiState.isLoading,
                            showAddProfileSheet = uiState.showAddProfileSheet,
                            showProfilePickerSheet = uiState.showProfilePickerSheet,
                            updatingProfileId = uiState.updatingProfileId,
                            updatedProfileId = uiState.updatedProfileId,
                            onProfileSelected = viewModel::selectProfile,
                            onProfileEdit = viewModel::editProfile,
                            onProfileDelete = viewModel::deleteProfile,
                            onProfileShare = viewModel::shareProfile,
                            onProfileShareURL = viewModel::shareProfileURL,
                            onProfileUpdate = viewModel::updateProfile,
                            onProfileMove = viewModel::moveProfile,
                            onShowAddProfileSheet = viewModel::showAddProfileSheet,
                            onHideAddProfileSheet = viewModel::hideAddProfileSheet,
                            onShowProfilePickerSheet = viewModel::showProfilePickerSheet,
                            onHideProfilePickerSheet = viewModel::hideProfilePickerSheet,
                            onOpenNewProfile = onOpenNewProfile,
                            commandClient = viewModel.commandClient,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Process cards for rendering, grouping consecutive half-width cards into rows
 */
fun processCardsForRendering(
    cardOrder: List<CardGroup>,
    visibleCards: Set<CardGroup>,
    cardWidths: Map<CardGroup, CardWidth>,
): List<CardRenderItem> {
    val renderItems = mutableListOf<CardRenderItem>()
    val visibleOrderedCards = cardOrder.filter { visibleCards.contains(it) }

    var i = 0
    while (i < visibleOrderedCards.size) {
        val currentCard = visibleOrderedCards[i]
        val currentWidth = cardWidths[currentCard] ?: CardWidth.Full

        if (currentWidth == CardWidth.Half) {
            // Check if next card is also half-width
            if (i + 1 < visibleOrderedCards.size) {
                val nextCard = visibleOrderedCards[i + 1]
                val nextWidth = cardWidths[nextCard] ?: CardWidth.Full

                if (nextWidth == CardWidth.Half) {
                    // Group two half-width cards together
                    renderItems.add(
                        CardRenderItem(
                            cards = listOf(currentCard, nextCard),
                            isRow = true,
                        ),
                    )
                    i += 2
                    continue
                }
            }
            // Single half-width card
            renderItems.add(
                CardRenderItem(
                    cards = listOf(currentCard),
                    isRow = false,
                ),
            )
        } else {
            // Full-width card
            renderItems.add(
                CardRenderItem(
                    cards = listOf(currentCard),
                    isRow = false,
                ),
            )
        }
        i++
    }

    return renderItems
}

/**
 * Determine if a service-dependent card has data available to display.
 * This function is only relevant when the service is running.
 * Note: Profiles card is always available and should not use this function.
 */
fun isCardAvailableWhenServiceRunning(cardGroup: CardGroup, uiState: DashboardUiState): Boolean = when (cardGroup) {
    CardGroup.ClashMode -> uiState.clashModeVisible
    CardGroup.UploadTraffic -> uiState.trafficVisible
    CardGroup.DownloadTraffic -> uiState.trafficVisible
    CardGroup.Debug -> true // Debug info is always available when service is running
    CardGroup.Connections -> uiState.trafficVisible
    CardGroup.SystemProxy -> uiState.systemProxyVisible
    CardGroup.Profiles -> true // This shouldn't be called for Profiles, but return true for safety
}

/**
 * NPV-style read-only "Active Configuration" summary for Home — just the
 * selected profile's name plus downlink/uplink totals. Full profile
 * management (add/edit/share/QR/update) lives in the Configs tab now; tapping
 * this card takes you there instead of opening inline actions here.
 */
@Composable
private fun ActiveConfigSummary(
    profile: Profile?,
    onClick: () -> Unit,
) {
    var configDetails by remember(profile?.id, profile?.typed?.path) {
        mutableStateOf(ActiveConfigDetails())
    }

    LaunchedEffect(profile?.id, profile?.typed?.path) {
        configDetails = inspectActiveConfig(profile)
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "ACTIVE CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Deliberate blank line after the heading.
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(top = 14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = profile?.name ?: "Not Set",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = configDetails.protocolSetup,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = configDetails.serverAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = configDetails.pingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class ActiveConfigDetails(
    val protocolSetup: String = "—",
    val serverAddress: String = "—",
    val pingText: String = "Ping — —",
)

private suspend fun inspectActiveConfig(
    profile: Profile?,
): ActiveConfigDetails = withContext(Dispatchers.IO) {
    if (profile == null) {
        return@withContext ActiveConfigDetails()
    }

    try {
        val file = File(profile.typed.path)

        if (!file.exists() || !file.isFile) {
            return@withContext ActiveConfigDetails()
        }

        val root = JSONObject(file.readText())

        var protocol = ""
        var transport = ""
        var tlsEnabled = false
        var realityEnabled = false
        var server = ""
        var port = -1

        // ----------------------------------------------------
        // Normal sing-box outbound profiles
        // ----------------------------------------------------

        val outbounds = root.optJSONArray("outbounds")

        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                val type = outbound.optString("type", "")

                if (
                    type.isEmpty() ||
                    type in setOf(
                        "direct",
                        "block",
                        "dns",
                        "selector",
                        "urltest",
                        "tun",
                        "mixed",
                        "socks",
                        "http",
                        "redirect",
                        "tproxy",
                    )
                ) {
                    continue
                }

                protocol = type
                server = outbound.optString("server", "")
                port = outbound.optInt("server_port", -1)

                val transportObject = outbound.optJSONObject("transport")

                if (transportObject != null) {
                    transport = transportObject.optString("type", "")
                }

                val tlsObject = outbound.optJSONObject("tls")

                if (tlsObject != null) {
                    tlsEnabled = tlsObject.optBoolean("enabled", false)

                    val realityObject = tlsObject.optJSONObject("reality")

                    if (realityObject != null) {
                        realityEnabled = realityObject.optBoolean("enabled", false)
                    }
                }

                break
            }
        }

        // ----------------------------------------------------
        // Endpoint profiles such as WireGuard/OpenVPN/
        // OpenConnect.
        // ----------------------------------------------------

        if (server.isEmpty()) {
            val endpoints = root.optJSONArray("endpoints")

            if (endpoints != null) {
                for (i in 0 until endpoints.length()) {
                    val endpoint = endpoints.optJSONObject(i) ?: continue
                    val type = endpoint.optString("type", "")

                    if (type.isEmpty()) {
                        continue
                    }

                    if (protocol.isEmpty()) {
                        protocol = type
                    }

                    server = endpoint.optString("server", "")
                    port = endpoint.optInt("server_port", -1)

                    // WireGuard endpoint format can store the
                    // remote address/port inside peers[].
                    if (server.isEmpty()) {
                        val peers = endpoint.optJSONArray("peers")
                        val peer = peers?.optJSONObject(0)

                        if (peer != null) {
                            server = peer.optString("address", "")
                            port = peer.optInt("port", -1)
                        }
                    }

                    break
                }
            }
        }

        // ----------------------------------------------------
        // Build the protocol/setup label ONLY from fields that
        // actually exist in the saved sing-box JSON.
        // ----------------------------------------------------

        val setupParts = mutableListOf<String>()

        if (protocol.isNotEmpty()) {
            setupParts += protocol.uppercase()
        }

        if (transport.isNotEmpty()) {
            setupParts += transport.uppercase()
        }

        if (tlsEnabled) {
            setupParts += "TLS"
        }

        if (realityEnabled) {
            setupParts += "REALITY"
        }

        val protocolSetup =
            if (setupParts.isNotEmpty()) {
                setupParts.joinToString(" • ")
            } else {
                "—"
            }

        // ----------------------------------------------------
        // Server address comes directly from the saved config.
        // ----------------------------------------------------

        val serverAddress =
            when {
                server.isNotEmpty() && port > 0 ->
                    "$server:$port"

                server.isNotEmpty() ->
                    server

                else ->
                    "—"
            }

        // ----------------------------------------------------
        // Real TCP connection timing.
        //
        // This is NOT a fabricated ping.
        // ProfilePingTester performs a real Socket.connect()
        // to the configured server and port.
        // ----------------------------------------------------

        val pingText =
            if (server.isNotEmpty() && port > 0) {
                when (val ping = ProfilePingTester.ping(server, port)) {
                    is PingResult.Success ->
                        "Ping — ${ping.millis} ms"

                    is PingResult.Failure ->
                        "Ping — —"
                }
            } else {
                "Ping — —"
            }

        ActiveConfigDetails(
            protocolSetup = protocolSetup,
            serverAddress = serverAddress,
            pingText = pingText,
        )
    } catch (_: Exception) {
        // Never guess if the saved configuration cannot be read.
        ActiveConfigDetails()
    }
}

