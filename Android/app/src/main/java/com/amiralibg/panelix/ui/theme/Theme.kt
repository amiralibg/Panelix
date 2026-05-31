package com.amiralibg.panelix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Ink80,
    secondary = Steel80,
    tertiary = Ember80,
    background = Color(0xFF0E1111),
    surface = Color(0xFF151919),
    surfaceVariant = Color(0xFF293031),
    primaryContainer = Color(0xFF0B4C4B),
    secondaryContainer = Color(0xFF34434A),
)

private val LightColorScheme = lightColorScheme(
    primary = Ink40,
    secondary = Steel40,
    tertiary = Ember40,
    background = Color(0xFFF7F9F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5ECEB),
    primaryContainer = Color(0xFFCBEDEA),
    secondaryContainer = Color(0xFFDCE5E9),
)

@Composable
fun PanelixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun PanelixThemePreview() {
    PanelixTheme {
        Surface(Modifier.padding(16.dp)) {
            Text(
                text = "Panelix theme",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
