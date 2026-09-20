package com.divitiae.pulsesync.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.divitiae.pulsesync.data.auth.GoogleSignInHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

/**
 * Handles the Google Sign-In **intent** round-trip for Compose.
 *
 * Returns a zero-arg lambda; call it from the "Continue with Google" button.
 * It launches the account picker via `StartActivityForResult`, then extracts
 * the Google ID token from the returned intent and hands it to [onIdToken].
 * Anything else (cancel, misconfiguration, Play Services error) goes to
 * [onFailure] — null meaning "user cancelled, do nothing".
 *
 * Uses the GoogleSignInClient that Member 3 already configured in
 * [GoogleSignInHelper] (which requests the ID token for the Firebase web
 * client id generated from google-services.json).
 */

@Composable
fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onFailure: (GoogleSignInFailure?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    // Keep the latest callbacks without re-registering the launcher on every recomposition.
    val currentOnIdToken by rememberUpdatedState(onIdToken)
    val currentOnFailure by rememberUpdatedState(onFailure)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.data == null && result.resultCode != Activity.RESULT_OK) {