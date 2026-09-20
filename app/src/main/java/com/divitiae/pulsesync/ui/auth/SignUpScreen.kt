package com.divitiae.pulsesync.ui.auth

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The Checkbox with toggleable row, stateful/stateless screen split, keyboard handling and preview annotations in this file were adapted from:
 *
 * Android Developers (2026) Checkbox. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/checkbox
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Where to hoist state. [online]
 * Available at: https://developer.android.com/develop/ui/compose/state-hoisting
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Configure text fields. [online]
 * Available at: https://developer.android.com/develop/ui/compose/text/user-input
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Preview your UI with composable previews. [online]
 * Available at: https://developer.android.com/develop/ui/compose/tooling/previews
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.components.BrandMark
import com.divitiae.pulsesync.ui.components.FooterPrompt
import com.divitiae.pulsesync.ui.components.GoogleAuthButton
import com.divitiae.pulsesync.ui.components.LabelledDivider
import com.divitiae.pulsesync.ui.components.PrimaryButton
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.components.PulseSyncTextField
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

/**
 * Sign Up (Figma frame 12:35, dark variant 29:443).
 * Same state-owning / callback-delegating shape as [SignInScreen].
 */
@Composable
fun SignUpScreen(
    onCreateAccount: (fullName: String, email: String, password: String) -> Unit,
    onGoogleSignUp: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = if (submitted && fullName.isBlank()) stringResource(R.string.sign_up_error_name_required) else null
    val emailError = when {
        !submitted -> null
        email.isBlank() -> stringResource(R.string.auth_error_email_required)
        !AuthValidation.isEmailValid(email) -> stringResource(R.string.auth_error_email_invalid)
        else -> null
    }
    val passwordError = when {
        !submitted -> null
        password.isBlank() -> stringResource(R.string.auth_error_password_required)
        !AuthValidation.isPasswordLongEnough(password) -> stringResource(R.string.auth_error_password_short)
        else -> null
    }
    val termsError = if (submitted && !termsAccepted) stringResource(R.string.sign_up_error_terms) else null

    SignUpContent(
        fullName = fullName,
        onFullNameChange = { fullName = it },
        nameError = nameError,
        email = email,
        onEmailChange = { email = it },
        emailError = emailError,
        password = password,
        onPasswordChange = { password = it },
        passwordError = passwordError,
        termsAccepted = termsAccepted,
        onTermsAcceptedChange = { termsAccepted = it },
        termsError = termsError,
        onSubmit = {
            submitted = true
            val valid = fullName.isNotBlank() &&
                AuthValidation.isEmailValid(email) &&
                AuthValidation.isPasswordLongEnough(password) &&
                termsAccepted
            if (valid) onCreateAccount(fullName.trim(), email.trim(), password)
        },
        onGoogleSignUp = onGoogleSignUp,
        onNavigateToSignIn = onNavigateToSignIn,
        modifier = modifier,
    )
}

@Composable
fun SignUpContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    nameError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    termsError: String?,
    onSubmit: () -> Unit,
    onGoogleSignUp: () -> Unit,
    onNavigateToSignIn: () -> Unit,
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
            // Header (Figma 12:42): 24 top / 16 bottom, 8 gap, smaller mark.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandMark(size = 52.dp, cornerRadius = 16.dp, iconSize = 24.dp)
                Text(
                    text = stringResource(R.string.sign_up_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.sign_up_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }

            // Form (Figma 12:47): 16 side padding, 12 gap.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PulseSyncDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GoogleAuthButton(
                    text = stringResource(R.string.sign_up_google),
                    onClick = onGoogleSignUp,
                )
                LabelledDivider(text = stringResource(R.string.sign_up_divider))
                PulseSyncTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    placeholder = stringResource(R.string.sign_up_name_placeholder),
                    leadingIcon = Icons.Rounded.Person,
                    errorText = nameError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
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
                    placeholder = stringResource(R.string.sign_up_password_placeholder),
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
                TermsRow(
                    checked = termsAccepted,
                    onCheckedChange = onTermsAcceptedChange,
                    errorText = termsError,
                )
                PrimaryButton(
                    text = stringResource(R.string.sign_up_button),
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                )
                FooterPrompt(
                    prompt = stringResource(R.string.sign_up_have_account),
                    action = stringResource(R.string.sign_up_go_to_sign_in),
                    onActionClick = onNavigateToSignIn,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                )
            }
        }
    }
}

/** Teal 18 dp checkbox with caption (Figma 12:64). */
@Composable
private fun TermsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    errorText: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Adapted from: Android Developers (2026) Checkbox. https://developer.android.com/develop/ui/compose/components/checkbox
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(18.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedColor = if (errorText != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                ),
            )
            Text(
                text = stringResource(R.string.sign_up_terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 26.dp, top = 4.dp),
            )
        }
    }
}

@Preview(name = "Sign Up · Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Sign Up · Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SignUpPreview() {
    PulseSyncTheme {
        SignUpContent(
            fullName = "",
            onFullNameChange = {},
            nameError = null,
            email = "",
            onEmailChange = {},
            emailError = null,
            password = "",
            onPasswordChange = {},
            passwordError = null,
            termsAccepted = true,
            onTermsAcceptedChange = {},
            termsError = null,
            onSubmit = {},
            onGoogleSignUp = {},
            onNavigateToSignIn = {},
        )
    }
}
