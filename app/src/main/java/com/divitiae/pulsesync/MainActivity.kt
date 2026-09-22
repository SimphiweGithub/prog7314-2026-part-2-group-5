package com.divitiae.pulsesync

/**
 * Code Attribution No 9
 * This method was taken from "Display content edge-to-edge in views"
 * https://developer.android.com/develop/ui/views/layout/edge-to-edge
 * Android Developers
 */

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.divitiae.pulsesync.data.domain.ThemePref
import com.divitiae.pulsesync.ui.navigation.PulseSyncNavHost
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
 * App root. The theme preference is observed straight from DataStore, so a
 * change made on the Settings screen re-themes every screen at once and is
 * still in effect after the process is killed and relaunched.
 */
@Composable
fun PulseSyncApp() {
    val container = (LocalContext.current.applicationContext as PulseSyncApplication).container
    // null until DataStore emits its first value; fall back to the system theme meanwhile.
    val preferences by container.preferencesRepository.preferences.collectAsState(initial = null)
    val darkTheme = when (preferences?.themeMode) {
        ThemePref.LIGHT -> false
        ThemePref.DARK -> true
        ThemePref.SYSTEM, null -> isSystemInDarkTheme()
    }
    PulseSyncTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PulseSyncNavHost(modifier = Modifier.fillMaxSize())
        }
    }
}
