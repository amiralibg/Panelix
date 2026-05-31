package com.amiralibg.panelix.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.PanelixTheme

@Composable
fun OnboardingScreen(onFolderPicked: (Uri, Int) -> Unit) {
    val launcher = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onFolderPicked(result.uri, result.flags)
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Panelix", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("A local-first comic reader for folders, PDF, CBZ, CBR, and image collections.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Step("1", "Choose a folder")
        Step("2", "Panelix scans your local library")
        Step("3", "Open comics and continue reading")
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { launcher.launch(Unit) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Outlined.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Choose folder")
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.width(28.dp).height(28.dp), contentAlignment = Alignment.Center) {
                    Text(number, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun OnboardingScreenPreview() {
    PanelixTheme {
        OnboardingScreen(onFolderPicked = { _, _ -> })
    }
}
