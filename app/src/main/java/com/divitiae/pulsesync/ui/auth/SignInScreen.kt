package com.divitiae.pulsesync.ui.auth

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The stateful/stateless screen split, keyboard options/actions, safe-drawing padding and preview annotations in this file were adapted from:
 *
 * Android Developers (2026) Where to hoist state. [online]
 * Available at: https://developer.android.com/develop/ui/compose/state-hoisting
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Configure text fields. [online]
 * Available at: https://developer.android.com/develop/ui/compose/text/user-input
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) About window insets. [online]
 * Available at: https://developer.android.com/develop/ui/compose/system/insets
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Preview your UI with composable previews. [online]
 * Available at: https://developer.android.com/develop/ui/compose/tooling/previews
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.components.BiometricAuthButton
import com.divitiae.pulsesync.ui.components.BrandMark
import com.divitiae.pulsesync.ui.components.FooterPrompt
import com.divitiae.pulsesync.ui.components.GoogleAuthButton
import com.divitiae.pulsesync.ui.components.LabelledDivider
import com.divitiae.pulsesync.ui.components.LinkText
import com.divitiae.pulsesync.ui.components.PrimaryButton
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.components.PulseSyncTextField
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

/**
 * Sign In (Figma frame 11:35, dark variant 29:410).
 *
 * Owns its form state so it can be previewed and navigated to on its own.
 * Auth calls are delegated through callbacks so Member 4 can plug a ViewModel
 * in without touching the layout.
 */
@Composable
fun SignInScreen(
    onSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onBiometricSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val emailError = when {
        !submitted -> null
        email.isBlank() -> stringResource(R.string.auth_error_email_required)
        !AuthValidation.isEmailValid(email) -> stringResource(R.string.auth_error_email_invalid)
        else -> null
    }
    val passwordError = when {
        !submitted -> null
        password.isBlank() -> stringResource(R.string.auth_error_password_required)
        else -> null
    }

    SignInContent(
        email = email,
        onEmailChange = { email = it },
        emailError = emailError,
        password = password,
        onPasswordChange = { password = it },
        passwordError = passwordError,
        onSubmit = {
            submitted = true
            if (AuthValidation.isEmailValid(email) && password.isNotBlank()) {
                onSignIn(email.trim(), password)
            }
        },
        onGoogleSignIn = onGoogleSignIn,
        onBiometricSignIn = onBiometricSignIn,
        onForgotPassword = onForgotPassword,
        onNavigateToSignUp = onNavigateToSignUp,
        modifier = modifier,
    )
}

@Composable
fun SignInContent(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onBiometricSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header (Figma 11:42): 48 top / 20 bottom, 10 gap.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark(size = 64.dp, cornerRadius = 20.dp, iconSize = 30.dp)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.auth_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }

            // Form (Figma 11:47): 16 side padding, 14 gap.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PulseSyncDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GoogleAuthButton(
                    text = stringResource(R.string.sign_in_google),
                    onClick = onGoogleSignIn,
                )
                BiometricAuthButton(
                    text = stringResource(R.string.sign_in_fingerprint),
                    onClick = onBiometricSignIn,
                )
                LabelledDivider(text = stringResource(R.string.sign_in_divider))
                PulseSyncTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    leadingIcon = Icons.Rounded.Email,
                    errorText = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                PulseSyncTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(R.string.auth_password_placeholder),
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                    errorText = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }),
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    LinkText(
                        text = stringResource(R.string.sign_in_forgot_password),
                        onClick = onForgotPassword,
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.sign_in_button),
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                )
                FooterPrompt(
                    prompt = stringResource(R.string.sign_in_no_account),
                    action = stringResource(R.string.sign_in_go_to_sign_up),
                    onActionClick = onNavigateToSignUp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Preview(name = "Sign In · Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Sign In · Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SignInPreview() {
    PulseSyncTheme {
        SignInContent(
            email = "",
            onEmailChange = {},
            emailError = null,
            password = "",
            onPasswordChange = {},
            passwordError = null,
            onSubmit = {},
            onGoogleSignIn = {},
            onBiometricSignIn = {},
            onForgotPassword = {},
            onNavigateToSignUp = {},
        )
    }
}

@Preview(name = "Sign In · Errors", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SignInErrorPreview() {
    PulseSyncTheme {
        SignInContent(
            email = "not-an-email",
            onEmailChange = {},
            emailError = "Enter a valid email address",
            password = "",
            onPasswordChange = {},
            passwordError = "Enter your password",
            onSubmit = {},
            onGoogleSignIn = {},
            onBiometricSignIn = {},
            onForgotPassword = {},
            onNavigateToSignUp = {},
        )
    }
}
