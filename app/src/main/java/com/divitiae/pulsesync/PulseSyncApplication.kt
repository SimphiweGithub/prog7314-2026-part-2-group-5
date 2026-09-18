package com.divitiae.pulsesync

import android.app.Application
import com.divitiae.pulsesync.data.di.AppContainer

/**
 * Application entry point. Builds the [AppContainer] once and exposes it to the
 * rest of the app; Member 4's ViewModels reach it via
 * `(application as PulseSyncApplication).container`.
 */
class PulseSyncApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.initialise()
    }
}
