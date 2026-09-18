package com.divitiae.pulsesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.divitiae.pulsesync.ui.navigation.PulseSyncNavHost
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseSyncTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PulseSyncApp()
                }
            }
        }
    }
}

@Composable
fun PulseSyncApp() {
    PulseSyncNavHost(modifier = Modifier.fillMaxSize())
}
