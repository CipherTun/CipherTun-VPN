package io.surprise.ciphertun.compose.screen.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.surprise.ciphertun.R
import io.surprise.ciphertun.compose.theme.CipherTunAccent
import io.surprise.ciphertun.constant.Status

@Composable
fun HeroConnectButton(
    serviceStatus: Status,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {},
) {
    val isRunning = serviceStatus == Status.Started
    val isStarting = serviceStatus == Status.Starting
    val isStopping = serviceStatus == Status.Stopping
    val isTransitioning = isStarting || isStopping

    val transition = rememberInfiniteTransition(
        label = "hero_transition",
    )

    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero_scale",
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero_glow",
    )

    val statusText = when (serviceStatus) {
        Status.Started -> "CONNECTED"
        Status.Starting -> "CONNECTING..."
        Status.Stopping -> "DISCONNECTING..."
        else -> "DISCONNECTED"
    }

    val statusColor = when {
        isRunning -> CipherTunAccent
        isTransitioning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 20.dp,
                    bottom = 22.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(178.dp)
                            .clip(CircleShape)
                            .background(
                                CipherTunAccent.copy(alpha = glowAlpha),
                            ),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(if (isRunning) pulse else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) {
                                CipherTunAccent
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable(
                            enabled = !isTransitioning,
                            interactionSource = remember {
                                MutableInteractionSource()
                            },
                            indication = null,
                            onClick = onToggle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isTransitioning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(52.dp),
                            strokeWidth = 4.dp,
                            color = CipherTunAccent,
                        )
                    } else {
                        Icon(
                            imageVector = if (isRunning) {
                                Icons.Default.Stop
                            } else {
                                Icons.Default.PowerSettingsNew
                            },
                            contentDescription = if (isRunning) {
                                stringResource(R.string.stop)
                            } else {
                                stringResource(R.string.action_start)
                            },
                            tint = if (isRunning) {
                                MaterialTheme.colorScheme.background
                            } else {
                                CipherTunAccent
                            },
                            modifier = Modifier.size(62.dp),
                        )
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
            )

            Text(
                text = when {
                    isRunning -> "CipherTun VPN is protecting your connection"
                    isStarting -> "Establishing secure connection"
                    isStopping -> "Closing VPN connection"
                    else -> "Tap the button to connect"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
