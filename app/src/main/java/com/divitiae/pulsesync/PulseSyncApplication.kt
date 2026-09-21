package com.divitiae.pulsesync

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
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
        Log.i(TAG, "Application onCreate: initializing AppContainer")
        /**
         * Code Attribution No 6
         * This method was taken from "Application.ActivityLifecycleCallbacks Logging Pattern"
         * https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks
         * Android Open Source Project
         */
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Log.d(TAG, "Activity created: ${activity.localClassName}")
            }

            override fun onActivityStarted(activity: Activity) {
                Log.d(TAG, "Activity started: ${activity.localClassName}")
            }

            override fun onActivityResumed(activity: Activity) {
                Log.d(TAG, "Activity resumed: ${activity.localClassName}")
            }

            override fun onActivityPaused(activity: Activity) {
                Log.d(TAG, "Activity paused: ${activity.localClassName}")
            }

            override fun onActivityStopped(activity: Activity) {
                Log.d(TAG, "Activity stopped: ${activity.localClassName}")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Log.d(TAG, "Activity saveInstanceState: ${activity.localClassName}")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Log.d(TAG, "Activity destroyed: ${activity.localClassName}")
            }
        })
        container = AppContainer(this)
        container.initialise()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Application onLowMemory received")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Application onTrimMemory: level=$level")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application onTerminate")
    }

    companion object {
        private const val TAG = "PulseSyncApp"
    }
}

