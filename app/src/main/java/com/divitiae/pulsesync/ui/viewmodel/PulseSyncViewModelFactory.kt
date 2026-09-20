package com.divitiae.pulsesync.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.divitiae.pulsesync.PulseSyncApplication
import com.divitiae.pulsesync.data.di.AppContainer

/**
 * Glue between Compose's `viewModel()` and Member 3's manual [AppContainer]
 * Each ViewModel exposes a `Factory` built with
 * [containerViewModelFactory]; the lambda receives the container and picks
 * the repositories it needs.
 *
 * Usage inside a composable:
 * ```
 * val vm: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
 * ```
 */
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) {
        "ViewModel created outside of PulseSyncApplication"
    } as PulseSyncApplication).container

inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create(appContainer) }
}