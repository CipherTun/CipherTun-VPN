package io.surprise.ciphertun.compose.screen.configs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.libbox.Libbox
import io.surprise.ciphertun.compose.component.qr.QRCodeDialog
import io.surprise.ciphertun.compose.navigation.NewProfileArgs
import io.surprise.ciphertun.compose.navigation.ProfileRoutes
import io.surprise.ciphertun.compose.screen.configuration.ProfileImportHandler
import io.surprise.ciphertun.compose.screen.dashboard.DashboardViewModel
import io.surprise.ciphertun.compose.screen.qrscan.QRScanResult
import io.surprise.ciphertun.compose.screen.qrscan.QRScanSheet
import io.surprise.ciphertun.compose.topbar.LocalScaffoldPadding
import io.surprise.ciphertun.compose.topbar.OverrideTopBar
import io.surprise.ciphertun.compose.util.QRCodeGenerator
import io.surprise.ciphertun.config.ProtocolType
import io.surprise.ciphertun.database.Profile
import io.surprise.ciphertun.ktx.errorDialogBuilder
import io.surprise.ciphertun.ktx.shareProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class ConfigSortOrder { NewestFirst, OldestFirst }

/**
 * CipherTun's Configs tab -- NPV-Tunnel-style: search, a collapsible "Saved"
 * group with sort/filter, per-row share/edit/delete + protocol tag, and a
 * global last-ping readout. Reuses DashboardViewModel so state stays in sync
 * with whatever's selected/running on Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigsScreen(
    navController: NavController,
    onOpenNewProfile: (NewProfileArgs) -> Unit,
    viewModel: DashboardViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importHandler = remember { ProfileImportHandler(context) }
    val scaffoldPadding = LocalScaffoldPadding.current

    var showAddSheet by remember { mutableStateOf(false) }
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showAddManuallySheet by remember { mutableStateOf(false) }
    var showQRScanSheet by remember { mutableStateOf(false) }
    var showShareSheetForProfile by remember { mutableStateOf<Profile?>(null) }
    var showQRCodeDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var savedExpanded by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(ConfigSortOrder.NewestFirst) }
    var hideFailedConfigs by remember { mutableStateOf(false) }

    var pingResults by remember { mutableStateOf<Map<Long, PingResult>>(emptyMap()) }
    var lastPingedProfileId by remember { mutableStateOf<Long?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    fun pingProfile(profile: Profile) {
        val summary = ProfileConfigInspector.summarize(profile)
        val address = summary.serverAddress
        if (address == null) {
            Toast.makeText(context, "Couldn't read a server address from this config", Toast.LENGTH_SHORT).show()
            return
        }
        val host = address.substringBeforeLast(":")
        val port = address.substringAfterLast(":").toIntOrNull() ?: return
        isPinging = true
        lastPingedProfileId = profile.id
        coroutineScope.launch {
            val result = ProfilePingTester.ping(host, port)
            pingResults = pingResults + (profile.id to result)
            isPinging = false
        }
    }

    val importFromFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                when (val result = importHandler.parseUri(uri)) {
                    is ProfileImportHandler.UriParseResult.Success -> {
                        when (val importResult = importHandler.importFromUri(uri)) {
                            is ProfileImportHandler.ImportResult.Success -> {}
                            is ProfileImportHandler.ImportResult.Error -> {
                                withContext(Dispatchers.Main) {
                                    context.errorDialogBuilder(Exception(importResult.message)).show()
                                }
                            }
                        }
                    }
                    is ProfileImportHandler.UriParseResult.Error -> {
                        withContext(Dispatchers.Main) {
                            context.errorDialogBuilder(Exception(result.message)).show()
                        }
                    }
                }
            }
        }
    }

    OverrideTopBar {
        TopAppBar(
            title = { Text("Configs") },
            actions = {
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add configuration")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(scaffoldPadding),
    ) {
        val lastPingResult = lastPingedProfileId?.let { pingResults[it] }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "LAST PING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when {
                        isPinging -> "Testing..."
                        lastPingResult is PingResult.Success -> "${lastPingResult.millis} ms"
                        lastPingResult is PingResult.Failure -> "Failed"
                        else -> "Never"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(
                enabled = !isPinging,
                onClick = {
                    val target = uiState.profiles.find { it.id == uiState.selectedProfileId }
                        ?: uiState.profiles.firstOrNull()
                    if (target == null) {
                        Toast.makeText(context, "No configs to ping yet", Toast.LENGTH_SHORT).show()
                    } else {
                        pingProfile(target)
                    }
                },
            ) {
                if (isPinging) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("PING")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search configs") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )

        Spacer(modifier = Modifier.padding(top = 4.dp))

        val filteredProfiles = uiState.profiles
            .filter { profile ->
                searchQuery.isBlank() || profile.name.contains(searchQuery, ignoreCase = true)
            }
            .filter { profile ->
                !hideFailedConfigs || pingResults[profile.id] !is PingResult.Failure
            }
            .let { list ->
                when (sortOrder) {
                    ConfigSortOrder.NewestFirst -> list.sortedByDescending { it.id }
                    ConfigSortOrder.OldestFirst -> list.sortedBy { it.id }
                }
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { savedExpanded = !savedExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (savedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
                Text(
                    text = "Saved (${uiState.profiles.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Row {
                IconButton(onClick = {
                    coroutineScope.launch {
                        for (profile in filteredProfiles) {
                            val summary = ProfileConfigInspector.summarize(profile)
                            val address = summary.serverAddress ?: continue
                            val host = address.substringBeforeLast(":")
                            val port = address.substringAfterLast(":").toIntOrNull() ?: continue
                            val result = ProfilePingTester.ping(host, port)
                            pingResults = pingResults + (profile.id to result)
                        }
                    }
                }) {
                    Icon(Icons.Default.Speed, contentDescription = "Test all")
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Sort and filter")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Newest added first") },
                            leadingIcon = {
                                RadioButton(selected = sortOrder == ConfigSortOrder.NewestFirst, onClick = null)
                            },
                            onClick = { sortOrder = ConfigSortOrder.NewestFirst },
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest added first") },
                            leadingIcon = {
                                RadioButton(selected = sortOrder == ConfigSortOrder.OldestFirst, onClick = null)
                            },
                            onClick = { sortOrder = ConfigSortOrder.OldestFirst },
                        )
                        DropdownMenuItem(
                            text = { Text("Hide failed configs") },
                            leadingIcon = {
                                Checkbox(checked = hideFailedConfigs, onCheckedChange = null)
                            },
                            onClick = { hideFailedConfigs = !hideFailedConfigs },
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.profiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    Text(
                        text = "[ NO CONFIGS ]",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "You haven't set up any configurations yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    )
                    TextButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(" Add Configuration", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else if (savedExpanded) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredProfiles, key = { it.id }) { profile ->
                        ConfigRow(
                            profile = profile,
                            selected = profile.id == uiState.selectedProfileId,
                            pingResult = pingResults[profile.id],
                            onClick = { viewModel.selectProfile(profile.id) },
                            onShare = { showShareSheetForProfile = profile },
                            onEdit = { viewModel.editProfile(profile) },
                            onDelete = { viewModel.deleteProfile(profile) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = "Add config",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        showAddSubscriptionDialog = true
                    },
                    leadingContent = { Icon(Icons.Outlined.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Add subscription") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (text.isNullOrBlank()) {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            onOpenNewProfile(NewProfileArgs(importUrl = text))
                        }
                    },
                    leadingContent = { Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Import config from Clipboard") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        showQRScanSheet = true
                    },
                    leadingContent = { Icon(Icons.Outlined.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Import config from QR Code") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        importFromFileLauncher.launch("*/*")
                    },
                    leadingContent = { Icon(Icons.Outlined.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Import config file") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        showAddManuallySheet = true
                    },
                    leadingContent = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Add config manually") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        Toast.makeText(context, "Connection groups are coming soon", Toast.LENGTH_SHORT).show()
                    },
                    leadingContent = { Icon(Icons.Outlined.GroupWork, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("New connection group") },
                )
            }
        }
    }

    if (showAddSubscriptionDialog) {
        var subName by remember { mutableStateOf("") }
        var subUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubscriptionDialog = false },
            title = { Text("Add subscription") },
            text = {
                Column {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Subscription name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("Subscription url") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddSubscriptionDialog = false
                        onOpenNewProfile(NewProfileArgs(importName = subName.ifBlank { null }, importUrl = subUrl))
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubscriptionDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showAddManuallySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddManuallySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = "Add config manually",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddManuallySheet = false
                        navController.navigate(ProfileRoutes.wizardProtocolPicker())
                    },
                    leadingContent = { Icon(Icons.Outlined.RssFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("V2Ray Config") },
                    supportingContent = { Text("VLESS, VMess, Trojan, Shadowsocks, and more") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddManuallySheet = false
                        navController.navigate(ProfileRoutes.wizardCredentialForm(ProtocolType.SSH.name))
                    },
                    leadingContent = { Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("SSH Config") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showAddManuallySheet = false
                        navController.navigate(ProfileRoutes.wizardCustomJson())
                    },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Custom (JSON)") },
                    supportingContent = { Text("Paste a complete sing-box config") },
                )
            }
        }
    }

    if (showQRScanSheet) {
        QRScanSheet(
            onDismiss = { showQRScanSheet = false },
            onScanResult = { result ->
                showQRScanSheet = false
                when (result) {
                    is QRScanResult.QRSData -> {
                        coroutineScope.launch {
                            when (val parseResult = importHandler.parseQRSData(result.data)) {
                                is ProfileImportHandler.QRSParseResult.Success -> {
                                    withContext(Dispatchers.Main) {
                                        importHandler.importFromQRSData(result.data)
                                    }
                                }
                                is ProfileImportHandler.QRSParseResult.Error -> {
                                    withContext(Dispatchers.Main) {
                                        context.errorDialogBuilder(Exception(parseResult.message)).show()
                                    }
                                }
                            }
                        }
                    }
                    is QRScanResult.RemoteProfile -> {
                        coroutineScope.launch {
                            when (val parseResult = importHandler.parseQRCode(result.uri.toString())) {
                                is ProfileImportHandler.QRCodeParseResult.RemoteProfile -> {
                                    withContext(Dispatchers.Main) {
                                        onOpenNewProfile(
                                            NewProfileArgs(importName = parseResult.name, importUrl = parseResult.url),
                                        )
                                    }
                                }
                                is ProfileImportHandler.QRCodeParseResult.LocalProfile -> {
                                    importHandler.importFromQRCode(result.uri.toString())
                                }
                                is ProfileImportHandler.QRCodeParseResult.Error -> {
                                    withContext(Dispatchers.Main) {
                                        context.errorDialogBuilder(Exception(parseResult.message)).show()
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    val shareProfileTarget = showShareSheetForProfile
    if (shareProfileTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheetForProfile = null },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = "Share configuration",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showShareSheetForProfile = null
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                context.shareProfile(shareProfileTarget)
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    context.errorDialogBuilder(e).show()
                                }
                            }
                        }
                    },
                    headlineContent = { Text("Export config") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showShareSheetForProfile = null
                        val link = Libbox.generateRemoteProfileImportLink(
                            shareProfileTarget.name,
                            shareProfileTarget.typed.remoteURL,
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Config URI", link))
                        Toast.makeText(context, "URI copied", Toast.LENGTH_SHORT).show()
                    },
                    headlineContent = { Text("Copy URI") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showShareSheetForProfile = null
                        try {
                            val json = File(shareProfileTarget.typed.path).readText()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Config JSON", json))
                            Toast.makeText(context, "JSON copied", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            context.errorDialogBuilder(e).show()
                        }
                    },
                    headlineContent = { Text("Copy JSON") },
                )
                ListItem(
                    modifier = Modifier.clickable {
                        showShareSheetForProfile = null
                        showQRCodeDialog = true
                    },
                    headlineContent = { Text("Show QR code") },
                )
                TextButton(
                    onClick = { showShareSheetForProfile = null },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                ) { Text("Cancel") }
            }
        }
    }

    if (showQRCodeDialog && shareProfileTarget != null) {
        val link = remember(shareProfileTarget) {
            Libbox.generateRemoteProfileImportLink(shareProfileTarget.name, shareProfileTarget.typed.remoteURL)
        }
        val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
        val qrBitmap = QRCodeGenerator.rememberPrimaryBitmap(link, backgroundColor = surfaceColor)
        QRCodeDialog(bitmap = qrBitmap, onDismiss = { showQRCodeDialog = false })
    }
}

@Composable
private fun ConfigRow(
    profile: Profile,
    selected: Boolean,
    pingResult: PingResult?,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val summary = remember(profile.typed.path) { ProfileConfigInspector.summarize(profile) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        headlineContent = { Text(profile.name) },
        supportingContent = {
            Column {
                Text(summary.serverAddress ?: "-")
                if (pingResult is PingResult.Failure) {
                    Text(
                        text = "Unreachable",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.IosShare, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
                if (summary.protocolType != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = summary.protocolType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}
