package io.github.innocentmiau.miautify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * A dark theme in Discord's colors, used whatever the phone's light or dark setting.
 * Material components read their colors from these roles:
 * - primary: filled buttons, the slider, active toggles.
 * - background, surface: the screen. surfaceContainer: the player bar.
 * - surfaceVariant: cover placeholders and the playing row.
 * - onSurfaceVariant: secondary text like artist and album.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Blurple,
    onPrimary = TextBright,
    primaryContainer = BlurpleDark,
    onPrimaryContainer = TextBright,
    secondary = TextMuted,
    onSecondary = Darkest,
    background = Background,
    onBackground = TextNormal,
    surface = Background,
    onSurface = TextNormal,
    surfaceVariant = Selected,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = Darkest,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = Raised,
    surfaceContainerHighest = Selected,
    outline = Outline,
    outlineVariant = Raised,
    error = Danger,
    onError = TextBright
)

@Composable
fun MiautifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
