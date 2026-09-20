package com.divitiae.pulsesync.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

/**
 * Builds the Google Sign-In client used to obtain a Google ID token, which
 * [AuthRepository] then exchanges through Firebase for a PulseSync JWT.
 *
 * The web client id is read at runtime from the `default_web_client_id` string
 * that the google-services plugin generates from google-services.json. Looking
 * it up by name (rather than a compile-time R.string reference) means the app
 * still builds if that resource is not present yet — sign-in simply won't work
 * until Firebase has a Google sign-in provider + this app's SHA-1 registered.
 */
object GoogleSignInHelper {

    fun client(context: Context): GoogleSignInClient =
        client(context, resolveWebClientId(context))

    fun client(context: Context, webClientId: String): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** True once Firebase has generated a usable web client id. */
    fun isConfigured(context: Context): Boolean = resolveWebClientId(context).isNotBlank()

    private fun resolveWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName,
        )
        return if (resId != 0) context.getString(resId) else ""
    }
}
