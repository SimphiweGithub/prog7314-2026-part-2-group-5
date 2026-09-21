package com.divitiae.pulsesync.ui.auth

/**
 * Code Attribution No 22
 * This method was taken from "Patterns and Email Address Validation in Android"
 * https://developer.android.com/reference/android/util/Patterns
 * Android Developers
 */

import android.util.Patterns

/**
 * Lightweight client-side checks for the auth forms. Server-side validation
 * lives behind the API (Members 3 and 4); these only keep the UI honest.
 */
internal object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    fun isEmailValid(email: String): Boolean =
        // Adapted from: Android Developers (2026) Patterns. https://developer.android.com/reference/android/util/Patterns
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isPasswordLongEnough(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH
}
