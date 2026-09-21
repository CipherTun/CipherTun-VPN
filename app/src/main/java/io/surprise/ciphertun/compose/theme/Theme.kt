package io.surprise.ciphertun.compose.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// CipherTun is forced dark theme only — no light scheme, no Material You dynamic color.
private val CipherTunColorScheme =
    darkColorScheme(
        primary = CipherTunAccent,
        onPrimary = CipherTunBackground,
        secondary = CipherTunAccentLight,
        tertiary = CipherTunAccentDark,
        background = CipherTunBackground,
        surface = CipherTunBackground,
    )

@Composable
fun Theme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CipherTunColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
