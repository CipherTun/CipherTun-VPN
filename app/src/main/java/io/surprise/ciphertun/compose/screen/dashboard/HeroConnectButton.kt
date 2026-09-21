package io.surprise.ciphertun.compose.screen.dashboard

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.surprise.ciphertun.R
import io.surprise.ciphertun.compose.theme.CipherTunAccent
import io.surprise.ciphertun.constant.Status

/**
 * CipherTun's signature hero connect/disconnect control.
 *
 * A large circular tap target, states:
 *  - Stopped: outlined ring, accent play icon, tap to connect
 *  - Starting/Stopping: outlined ring, spinner, disabled
 *  - Started: solid accent fill, stop icon, slow outward pulse
 */
@Composable
fun HeroConnectButton(
    serviceStatus: Status,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {},
) {
    val isRunning = serviceStatus == Status.Started
    val isTransitioning = serviceStatus == Status.Starting || serviceStatus == Status.Stopping
    val enabled = !isTransitioning

    val infiniteTransition = rememberInfiniteTransition(label = "hero_connect_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec =
        infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hero_connect_pulse_alpha",
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRunning) {
                Box(
                    modifier =
                    Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(CipherTunAccent.copy(alpha = pulseAlpha)),
                )
            }

            Box(
                modifier =
                Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) CipherTunAccent else Color.Transparent)
                    .border(width = 3.dp, color = CipherTunAccent, shape = CircleShape)
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isTransitioning -> {
                        CircularProgressIndicator(
                            color = CipherTunAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp),
                        )
                    }

                    isRunning -> {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.stop),
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(56.dp),
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_start),
                            tint = CipherTunAccent,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }

        Text(
            text =
            when (serviceStatus) {
                Status.Started -> stringResource(R.string.status_started)
                Status.Starting -> stringResource(R.string.status_starting)
                Status.Stopping -> stringResource(R.string.status_stopping)
                else -> stringResource(R.string.action_start)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
