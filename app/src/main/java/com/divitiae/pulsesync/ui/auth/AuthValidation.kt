package com.divitiae.pulsesync.ui.auth

import android.util.Patterns

/**
 * Lightweight client-side checks for the auth forms. Server-side validation
 * lives behind the API (Members 3 and 4); these only keep the UI honest.
 */
internal object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    fun isEmailValid(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isPasswordLongEnough(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH
}
