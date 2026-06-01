package com.amiralibg.panelix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.amiralibg.panelix.data.AccentColor

@Immutable
data class PanelixPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surfaceHi: Color,
    val line: Color,
    val lineHi: Color,
    val text: Color,
    val sub: Color,
    val muted: Color,
    val scrim: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LocalPanelixPalette = staticCompositionLocalOf {
    panelixPaletteFor(true, AccentColor.coral)
}

fun panelixPaletteFor(dark: Boolean, accent: AccentColor): PanelixPalette {
    val accentColor = when (accent) {
        AccentColor.coral -> PanelixAccents.coral
        AccentColor.teal -> PanelixAccents.teal
        AccentColor.violet -> PanelixAccents.violet
        AccentColor.amber -> PanelixAccents.amber
    }
    return if (dark) PanelixPalette(
        bg = PanelixDark.bg,
        surface = PanelixDark.surface,
        surface2 = PanelixDark.surface2,
        surfaceHi = PanelixDark.surfaceHi,
        line = PanelixDark.line,
        lineHi = PanelixDark.lineHi,
        text = PanelixDark.text,
        sub = PanelixDark.sub,
        muted = PanelixDark.muted,
        scrim = PanelixDark.scrim,
        accent = accentColor,
        onAccent = OnAccent,
        isDark = true,
    ) else PanelixPalette(
        bg = PanelixLight.bg,
        surface = PanelixLight.surface,
        surface2 = PanelixLight.surface2,
        surfaceHi = PanelixLight.surfaceHi,
        line = PanelixLight.line,
        lineHi = PanelixLight.lineHi,
        text = PanelixLight.text,
        sub = PanelixLight.sub,
        muted = PanelixLight.muted,
        scrim = PanelixLight.scrim,
        accent = accentColor,
        onAccent = OnAccent,
        isDark = false,
    )
}

private fun darkScheme(palette: PanelixPalette) = darkColorScheme(
    primary = palette.accent,
    onPrimary = palette.onAccent,
    primaryContainer = palette.accent,
    onPrimaryContainer = palette.onAccent,
    secondary = palette.accent,
    onSecondary = palette.onAccent,
    tertiary = palette.accent,
    background = palette.bg,
    onBackground = palette.text,
    surface = palette.surface,
    onSurface = palette.text,
    surfaceVariant = palette.surface2,
    onSurfaceVariant = palette.sub,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surface2,
    surfaceContainerHighest = palette.surfaceHi,
    outline = palette.lineHi,
    outlineVariant = palette.line,
    scrim = palette.scrim,
)

private fun lightScheme(palette: PanelixPalette) = lightColorScheme(
    primary = palette.accent,
    onPrimary = palette.onAccent,
    primaryContainer = palette.accent,
    onPrimaryContainer = palette.onAccent,
    secondary = palette.accent,
    onSecondary = palette.onAccent,
    tertiary = palette.accent,
    background = palette.bg,
    onBackground = palette.text,
    surface = palette.surface,
    onSurface = palette.text,
    surfaceVariant = palette.surface2,
    onSurfaceVariant = palette.sub,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surface2,
    surfaceContainerHighest = palette.surfaceHi,
    outline = palette.lineHi,
    outlineVariant = palette.line,
    scrim = palette.scrim,
)

@Composable
fun PanelixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.coral,
    content: @Composable () -> Unit
) {
    val palette = panelixPaletteFor(darkTheme, accent)
    val colorScheme = if (darkTheme) darkScheme(palette) else lightScheme(palette)

    CompositionLocalProvider(LocalPanelixPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
