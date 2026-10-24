package net.extrawdw.apps.zheteng.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
internal fun ZhetengTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val baseScheme = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = baseScheme.copy(
        primary = colors.ink, onPrimary = colors.paper, secondary = colors.coral,
        background = colors.paper, surface = colors.paper, onSurface = colors.ink,
        onBackground = colors.ink, onSurfaceVariant = colors.muted,
        surfaceContainer = colors.panel, outline = colors.line,
        primaryContainer = colors.paleGreen, onPrimaryContainer = colors.ink,
        secondaryContainer = colors.paleCoral, onSecondaryContainer = colors.ink
    )
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
