package io.surprise.ciphertun.compose.screen.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.surprise.ciphertun.compose.navigation.Screen
import io.surprise.ciphertun.compose.topbar.LocalScaffoldPadding
import io.surprise.ciphertun.compose.topbar.OverrideTopBar

/**
 * Top-level "More" tab — three category rows only. Each category's actual
 * features live one tap away (ConnectivityScreen / DiagnosticsScreen /
 * SettingsScreen) so this landing page stays short and uncluttered instead
 * of dumping every tool/setting onto one screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavController,
    showStatusBar: Boolean = false,
) {
    OverrideTopBar {
        TopAppBar(title = { Text("More") })
    }

    val scaffoldPadding = LocalScaffoldPadding.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(scaffoldPadding)
            .padding(top = 8.dp, bottom = if (showStatusBar) 74.dp else 8.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            ListItem(
                headlineContent = { Text("Connectivity", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Groups, connections, Tailscale, OpenConnect, OpenVPN, USBIP") },
                leadingContent = {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .clickable { navController.navigate("more/connectivity") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Diagnostics", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Network quality, STUN test, crash/OOM/power reports") },
                leadingContent = {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clickable { navController.navigate("more/diagnostics") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Settings", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("App, core, service, privilege, remote control") },
                leadingContent = {
                    Icon(Icons.Outlined.SettingsSuggest, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .clickable { navController.navigate(Screen.Settings.route) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}
