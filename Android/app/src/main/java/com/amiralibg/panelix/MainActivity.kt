package com.amiralibg.panelix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiralibg.panelix.data.ThemePreference
import com.amiralibg.panelix.di.AppGraph
import com.amiralibg.panelix.ui.navigation.PanelixApp
import com.amiralibg.panelix.ui.theme.PanelixTheme

class MainActivity : ComponentActivity() {
    private val graph by lazy { AppGraph(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by graph.repository.appPreferences.collectAsStateWithLifecycle(initialValue = com.amiralibg.panelix.data.AppPreferences())
            val dark = when (preferences.themePreference) {
                ThemePreference.system -> isSystemInDarkTheme()
                ThemePreference.light -> false
                ThemePreference.dark -> true
            }
            PanelixTheme(darkTheme = dark, accent = preferences.accentColor) {
                PanelixApp(graph.repository)
            }
        }
    }
}
