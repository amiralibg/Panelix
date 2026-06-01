package com.amiralibg.panelix.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette
import com.amiralibg.panelix.ui.theme.PanelixDisplayFont
import com.amiralibg.panelix.ui.theme.PanelixTheme

@Composable
fun OnboardingScreen(onFolderPicked: (Uri, Int) -> Unit) {
    val palette = LocalPanelixPalette.current
    val launcher = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onFolderPicked(result.uri, result.flags)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Panelix",
            style = TextStyle(
                fontFamily = PanelixDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                letterSpacing = (-0.8).sp,
                color = palette.text,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A calm, local-first comic reader for folders, PDF, CBZ, CBR, and image collections.",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.muted,
        )
        Spacer(Modifier.height(32.dp))
        Step("1", "Choose a folder")
        Spacer(Modifier.height(10.dp))
        Step("2", "Panelix scans your local library")
        Spacer(Modifier.height(10.dp))
        Step("3", "Open comics and continue reading")
        Spacer(Modifier.height(32.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accent)
                .clickable { launcher.launch(Unit) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.FolderOpen, null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Choose folder", color = palette.onAccent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = palette.accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, color = palette.text, style = MaterialTheme.typography.titleSmall)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E10)
@Composable
private fun OnboardingScreenPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        OnboardingScreen(onFolderPicked = { _, _ -> })
    }
}
