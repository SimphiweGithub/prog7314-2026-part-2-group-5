package com.divitiae.pulsesync.ui.auth

import android.app.Activity
import android.util.Log
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

private const val TAG = "GoogleSignInLauncher"

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
            Log.d(TAG, "Google Sign-In picker cancelled by user (resultCode=${result.resultCode})")
            currentOnFailure(null) // back-pressed out of the picker
            return@rememberLauncherForActivityResult
        }
        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Log.w(TAG, "Google Sign-In completed without an ID token")
                currentOnFailure(GoogleSignInFailure.NO_ID_TOKEN)
            } else {
                Log.i(TAG, "Google Sign-In successfully returned Google ID token")
                currentOnIdToken(idToken)
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                    Log.d(TAG, "Google Sign-In cancelled by user (status code ${e.statusCode})")
                    currentOnFailure(null)
                }
                // 10 = DEVELOPER_ERROR: almost always a missing SHA-1 in Firebase console.
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    Log.w(TAG, "Google Sign-In developer configuration error (status code 10/DEVELOPER_ERROR)", e)
                    currentOnFailure(GoogleSignInFailure.NOT_CONFIGURED)
                }
                else -> {
                    Log.e(TAG, "Google Sign-In API error with status code ${e.statusCode}", e)
                    currentOnFailure(GoogleSignInFailure.API_ERROR)
                }
            }
        }
    }
    return {
        if (!GoogleSignInHelper.isConfigured(context)) {
            Log.w(TAG, "Google Sign-In is not configured for this build (web client ID missing)")
            currentOnFailure(GoogleSignInFailure.NOT_CONFIGURED)
        } else {
            Log.d(TAG, "Launching Google Sign-In intent picker")
            val client = GoogleSignInHelper.client(context)
            // Sign out of the cached Google account first so the picker always
            // appears — otherwise a second tap silently reuses the last account.
            client.signOut().addOnCompleteListener {
                launcher.launch(client.signInIntent)
            }
        }
    }
}