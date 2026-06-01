package com.amiralibg.panelix.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.FolderEntity
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.data.ThemePreference
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette
import com.amiralibg.panelix.ui.theme.PanelixAccents
import com.amiralibg.panelix.ui.theme.PanelixDisplayFont
import com.amiralibg.panelix.ui.theme.PanelixTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAddFolder: (Uri, Int) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onReaderMode: (ReaderLayoutMode) -> Unit,
    onDirection: (ReadingDirection) -> Unit,
    onAccent: (AccentColor) -> Unit,
    onShowProgress: (Boolean) -> Unit,
    onKeepAwake: (Boolean) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    var pendingDelete by remember { mutableStateOf<FolderEntity?>(null) }
    val picker = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onAddFolder(result.uri, result.flags)
    }
    Scaffold(containerColor = palette.bg) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(palette.bg),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
        ) {
            item { TopBar(onBack = onBack) }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                SettingsGroup("Appearance") {
                    SegmentedRow(
                        title = "Theme",
                        value = state.preferences.themePreference,
                        options = listOf(
                            Segment(ThemePreference.system, "System", Icons.Outlined.Brightness6),
                            Segment(ThemePreference.light, "Light", Icons.Outlined.LightMode),
                            Segment(ThemePreference.dark, "Dark", Icons.Outlined.DarkMode),
                        ),
                        onChange = onTheme,
                    )
                    Divider()
                    AccentRow(value = state.preferences.accentColor, onChange = onAccent)
                    Divider()
                    SwitchRow(
                        title = "Show progress on covers",
                        sub = "Display a reading bar on in-progress comics",
                        checked = state.preferences.showProgressOnCovers,
                        onChange = onShowProgress,
                    )
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
            item {
                SettingsGroup("Reading") {
                    SegmentedRow(
                        title = "Default reader mode",
                        value = state.preferences.readerLayoutMode,
                        options = listOf(
                            Segment(ReaderLayoutMode.vertical, "Vertical", Icons.Outlined.UnfoldMore),
                            Segment(ReaderLayoutMode.horizontal, "Horizontal", Icons.Outlined.SwapHoriz),
                            Segment(ReaderLayoutMode.spread, "Spread", Icons.AutoMirrored.Outlined.ChromeReaderMode),
                        ),
                        onChange = onReaderMode,
                    )
                    Divider()
                    SegmentedRow(
                        title = "Reading direction",
                        value = state.preferences.readingDirection,
                        options = listOf(
                            Segment(ReadingDirection.ltr, "Left → Right", Icons.AutoMirrored.Outlined.ArrowForward),
                            Segment(ReadingDirection.rtl, "Right → Left", Icons.AutoMirrored.Outlined.ArrowBack),
                        ),
                        onChange = onDirection,
                    )
                    Divider()
                    SwitchRow(
                        title = "Keep screen awake",
                        sub = "Prevent dimming while reading",
                        checked = state.preferences.keepScreenAwake,
                        onChange = onKeepAwake,
                    )
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
            item {
                SettingsGroup("Library folders") {
                    Text(
                        "Permissions are granted per folder. If access is revoked, add the folder again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                    Divider()
                    state.folders.forEach { folder ->
                        FolderRow(
                            folder = folder,
                            onRemove = { pendingDelete = folder },
                        )
                        Divider()
                    }
                    AddFolderRow(onClick = { picker.launch(Unit) })
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
            item {
                SettingsGroup("Library") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRescan)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Rescan library", style = MaterialTheme.typography.titleSmall, color = palette.text)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Refresh covers and page counts from disk",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                        Text("Run", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    pendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = palette.surface,
            title = { Text("Remove folder?", color = palette.text) },
            text = { Text(folder.name, color = palette.muted) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFolder(folder.uri)
                    pendingDelete = null
                }) { Text("Remove", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = palette.muted)
                }
            },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = palette.text, modifier = Modifier.size(22.dp))
        }
        Text(
            "Settings",
            style = TextStyle(
                fontFamily = PanelixDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                letterSpacing = (-0.3).sp,
                color = palette.text,
            ),
        )
    }
}

@Composable
private fun SettingsGroup(label: String, content: @Composable () -> Unit) {
    val palette = LocalPanelixPalette.current
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .border(1.dp, palette.line, RoundedCornerShape(18.dp)),
        ) { content() }
    }
}

@Composable
private fun Divider() {
    val palette = LocalPanelixPalette.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
}

private data class Segment<T>(val key: T, val label: String, val icon: ImageVector)

@Composable
private fun <T> SegmentedRow(
    title: String,
    value: T,
    options: List<Segment<T>>,
    onChange: (T) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = palette.text)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(palette.surface2)
                .border(1.dp, palette.line, RoundedCornerShape(13.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { opt ->
                val sel = opt.key == value
                Row(
                    Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (sel) palette.accent else Color.Transparent)
                        .clickable { onChange(opt.key) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        opt.icon,
                        null,
                        tint = if (sel) palette.onAccent else palette.sub,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        opt.label,
                        color = if (sel) palette.onAccent else palette.sub,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentRow(value: AccentColor, onChange: (AccentColor) -> Unit) {
    val palette = LocalPanelixPalette.current
    val order = listOf(AccentColor.coral, AccentColor.teal, AccentColor.violet, AccentColor.amber)
    val labels = mapOf(
        AccentColor.coral to "Coral",
        AccentColor.teal to "Teal",
        AccentColor.violet to "Violet",
        AccentColor.amber to "Amber",
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Accent color", style = MaterialTheme.typography.titleSmall, color = palette.text, maxLines = 1)
            Text(labels[value] ?: "", style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            order.forEach { acc ->
                val color = when (acc) {
                    AccentColor.coral -> PanelixAccents.coral
                    AccentColor.teal -> PanelixAccents.teal
                    AccentColor.violet -> PanelixAccents.violet
                    AccentColor.amber -> PanelixAccents.amber
                }
                val selected = acc == value
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = if (selected) palette.bg else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { onChange(acc) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    sub: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.text)
            if (sub != null) {
                Spacer(Modifier.height(3.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
        Spacer(Modifier.width(14.dp))
        PanelixSwitch(on = checked)
    }
}

@Composable
private fun PanelixSwitch(on: Boolean) {
    val palette = LocalPanelixPalette.current
    val offset by animateDpAsState(targetValue = if (on) 21.dp else 3.dp, label = "switch")
    Box(
        Modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) palette.accent else palette.surfaceHi),
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun FolderRow(folder: FolderEntity, onRemove: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Folder, null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.titleSmall, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                folder.uri,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Delete, "Remove", tint = palette.muted, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun AddFolderRow(onClick: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Add, null, tint = palette.accent, modifier = Modifier.size(19.dp))
        Text("Add folder", color = palette.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E10)
@Composable
private fun SettingsScreenPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        SettingsScreen(
            state = SettingsUiState(
                preferences = AppPreferences(
                    themePreference = ThemePreference.dark,
                    readerLayoutMode = ReaderLayoutMode.horizontal,
                    readingDirection = ReadingDirection.ltr,
                    accentColor = AccentColor.coral,
                ),
                folders = listOf(
                    FolderEntity(id = 1, uri = "/storage/emulated/0/Comics", name = "Comics"),
                ),
            ),
            onBack = {},
            onAddFolder = { _, _ -> },
            onRemoveFolder = {},
            onRescan = {},
            onTheme = {},
            onReaderMode = {},
            onDirection = {},
            onAccent = {},
            onShowProgress = {},
            onKeepAwake = {},
        )
    }
}
