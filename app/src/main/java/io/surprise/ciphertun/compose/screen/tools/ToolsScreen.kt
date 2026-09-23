package io.surprise.ciphertun.compose.screen.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ToolsScreen(
    navController: NavController,
    showStatusBar: Boolean = false,
) {
    OverrideTopBar {
        TopAppBar(
            title = { Text("Tools") },
        )
    }

    val scaffoldPadding = LocalScaffoldPadding.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(scaffoldPadding)
            .padding(
                top = 8.dp,
                bottom = if (showStatusBar) 74.dp else 8.dp,
            ),
    ) {
        SectionTitle("Settings")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                supportingContent = {
                    Text("App, core, service, profile override, remote control and privilege")
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        navController.navigate(Screen.Settings.route)
                    },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
        }

        SectionTitle("Network")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            ListItem(
                headlineContent = {
                    Text("Connectivity")
                },
                supportingContent = {
                    Text("Connections, groups, Tailscale, OpenVPN, OpenConnect and USB/IP")
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .clickable {
                        navController.navigate("tools/connectivity")
                    },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )

            ListItem(
                headlineContent = {
                    Text("Network Quality")
                },
                supportingContent = {
                    Text("Test network latency and reachability")
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.clickable {
                    navController.navigate("tools/network_quality")
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )

            ListItem(
                headlineContent = {
                    Text("STUN Test")
                },
                supportingContent = {
                    Text("Inspect NAT and connectivity information")
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .clickable {
                        navController.navigate("tools/stun_test")
                    },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
        }

        SectionTitle("Diagnostics")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            ListItem(
                headlineContent = {
                    Text("Diagnostics")
                },
                supportingContent = {
                    Text("Network diagnostics and troubleshooting tools")
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        navController.navigate("more/diagnostics")
                    },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = 32.dp,
            vertical = 8.dp,
        ),
    )
}
