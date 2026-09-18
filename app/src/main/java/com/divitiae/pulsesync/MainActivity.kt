package com.divitiae.pulsesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.divitiae.pulsesync.ui.navigation.PulseSyncNavHost
import com.divitiae.pulsesync.ui.settings.ThemeMode
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PulseSyncApp() }
    }
}

/**
 * App root. The theme preference from Settings is held here so a change
 * re-themes every screen at once; Member 4 can back it with DataStore.
 */
@Composable
fun PulseSyncApp() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    PulseSyncTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PulseSyncNavHost(
                themeMode = themeMode,
                onThemeModeChange = { themeMode = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
