package com.divitiae.pulsesync

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The edge-to-edge setup, Material 3 theme wrapper and the rememberSaveable theme state in this file were adapted from:
 *
 * Android Developers (2026) Display content edge-to-edge in views. [online]
 * Available at: https://developer.android.com/develop/ui/views/layout/edge-to-edge
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Material Design 3 in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/designsystems/material3
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) State and Jetpack Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/state
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.os.Bundle
import android.util.Log
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
        Log.i(TAG, "onCreate: savedInstanceState is ${if (savedInstanceState == null) "null" else "non-null"}")
        // Adapted from: Android Developers (2026) Display content edge-to-edge in views. https://developer.android.com/develop/ui/views/layout/edge-to-edge
        enableEdgeToEdge()
        setContent { PulseSyncApp() }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")
    }

    companion object {
        private const val TAG = "MainActivity"
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
