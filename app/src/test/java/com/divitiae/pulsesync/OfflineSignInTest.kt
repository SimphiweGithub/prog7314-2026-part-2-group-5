package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.auth.AuthRepository
import com.divitiae.pulsesync.data.auth.AuthRepository.Companion.isServerUnavailable
import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.auth.SessionManager
import com.divitiae.pulsesync.data.auth.SignIn
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.data.local.dao.UserDao
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.remote.dto.AuthResponseDto
import com.divitiae.pulsesync.testutil.MainDispatcherRule
import com.divitiae.pulsesync.ui.auth.AuthEvent
import com.divitiae.pulsesync.ui.auth.AuthViewModel
import com.divitiae.pulsesync.ui.common.UiState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * The offline sign-in fallback must be visible, not silent: when Google and
 * Firebase accept the user but the PulseSync API is unreachable, the app
 * enters an offline session, tells the user, and never pretends a Firebase
 * ID token is an API token. A credential the API actively rejected is a real
 * failure and is not papered over.
 *
 * Code Attribution No 52
 * This method was taken from "Testing ViewModels with StateFlow and runTest"
 * https://developer.android.com/topic/architecture/ui-layer/state-production#testing
 * Android Developers
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSignInTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleUser = UserProfile(
        userId = "usr-test-123",
        email = "test.user@pulsesync.co.za",
        displayName = "Test User",
        photoUrl = null,
    )

    // ---- Classification -----------------------------------------------------------------

    @Test
    fun isServerUnavailable_distinguishesOutagesFromRejections() {
        assertTrue(AppError.Network("timeout").isServerUnavailable())
        assertTrue(AppError.Http(503, "cold start").isServerUnavailable())
        assertTrue(AppError.Http(404, "not deployed").isServerUnavailable())
        assertTrue(AppError.Unknown("boom").isServerUnavailable())
        assertFalse(AppError.Unauthorized("bad token").isServerUnavailable())
        assertFalse(AppError.Http(400, "bad request").isServerUnavailable())
    }

    // ---- AuthRepository -----------------------------------------------------------------

    private class RepoFixture(
        val api: PulseSyncApi,
        val userDao: UserDao,
        val tokenStore: AuthTokenStore,
        val firebaseAuth: FirebaseAuth,
        val repository: AuthRepository,
    )

    private fun repoFixture(): RepoFixture {
        val api: PulseSyncApi = mock()
        val userDao: UserDao = mock()
        val tokenStore: AuthTokenStore = mock()
        val firebaseAuth: FirebaseAuth = mock()
        val firebaseUser: FirebaseUser = mock()
        val authResult: AuthResult = mock()

        whenever(firebaseUser.uid).thenReturn("firebase-uid")
        whenever(firebaseUser.email).thenReturn("test.user@pulsesync.co.za")
        whenever(firebaseUser.displayName).thenReturn("Test User")
        whenever(firebaseUser.getIdToken(any())).thenReturn(Tasks.forResult(GetTokenResult("firebase-id-token", emptyMap())))
        whenever(authResult.user).thenReturn(firebaseUser)
        whenever(firebaseAuth.signInWithCredential(any())).thenReturn(Tasks.forResult(authResult))
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)

        val repository = AuthRepository(api, userDao, tokenStore, firebaseAuth, io = mainDispatcherRule.testDispatcher)
        return RepoFixture(api, userDao, tokenStore, firebaseAuth, repository)
    }

    @Test
    fun signIn_whenApiIsDown_entersOfflineSessionAndReportsIt() = runTest {
        val f = repoFixture()
        whenever(f.api.exchangeGoogleToken(any())).thenReturn(Response.error(503, "service unavailable".toResponseBody()))

        val result = f.repository.signInWithGoogleIdToken("google-id-token", fcmToken = null)

        assertTrue("User must still get in", result is Result.Success)
        val signIn = (result as Result.Success).data
        assertTrue("Outcome must be flagged offline", signIn.isOffline)
        assertTrue(signIn.serverError is AppError.Http)
        assertEquals("firebase-uid", signIn.profile.userId)
        verify(f.tokenStore).markOfflineSession()
        verify(f.tokenStore, never()).save(any(), any())
        verify(f.userDao).upsert(any())
    }

    @Test
    fun signIn_whenApiRejectsCredential_failsInsteadOfFallingBack() = runTest {
        val f = repoFixture()
        whenever(f.api.exchangeGoogleToken(any())).thenReturn(Response.error(401, "invalid token".toResponseBody()))

        val result = f.repository.signInWithGoogleIdToken("google-id-token", fcmToken = null)

        assertTrue("A rejected credential is a real failure", result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Unauthorized)
        verify(f.tokenStore, never()).markOfflineSession()
        verify(f.tokenStore, never()).save(any(), any())
        verify(f.userDao, never()).upsert(any())
    }

    @Test
    fun signIn_whenApiSucceeds_storesJwtAndIsOnline() = runTest {
        val f = repoFixture()
        whenever(f.api.exchangeGoogleToken(any()))
            .thenReturn(Response.success(AuthResponseDto(userId = "api-uid", accessToken = "jwt", refreshToken = "rt")))

        val result = f.repository.signInWithGoogleIdToken("google-id-token", fcmToken = null)

        val signIn = (result as Result.Success).data
        assertFalse(signIn.isOffline)
        assertNull(signIn.serverError)
        assertEquals("api-uid", signIn.profile.userId)
        verify(f.tokenStore).save("jwt", "rt")
        verify(f.tokenStore, never()).markOfflineSession()
    }

    @Test
    fun completePendingExchange_upgradesOfflineSessionOnReconnect() = runTest {
        val f = repoFixture()
        whenever(f.tokenStore.isOfflineSession).thenReturn(true)
        whenever(f.tokenStore.accessToken).thenReturn(null)
        whenever(f.api.exchangeGoogleToken(any()))
            .thenReturn(Response.success(AuthResponseDto(userId = "api-uid", accessToken = "jwt", refreshToken = "rt")))

        val result = f.repository.completePendingExchange()

        assertTrue(result is Result.Success)
        verify(f.tokenStore).save("jwt", "rt")
    }

    @Test
    fun completePendingExchange_isNoOpForOnlineSession() = runTest {
        val f = repoFixture()
        whenever(f.tokenStore.isOfflineSession).thenReturn(false)

        val result = f.repository.completePendingExchange()

        assertTrue(result is Result.Success)
        verify(f.api, never()).exchangeGoogleToken(any())
    }

    // ---- AuthViewModel ------------------------------------------------------------------

    @Test
    fun authViewModel_offlineSignIn_entersAppAndRaisesOfflineEvent() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        whenever(authRepository.signInWithGoogleIdToken(eq("google-id-token"), anyOrNull()))
            .thenReturn(Result.Success(SignIn(sampleUser, serverError = AppError.Network("Unable to resolve host"))))
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        viewModel.onGoogleIdToken("google-id-token")
        advanceUntilIdle()

        val state = viewModel.signInState.value
        assertTrue("Offline sign-in still navigates into the app", state is UiState.Success)
        assertEquals(sampleUser, (state as UiState.Success).data)
        val event = viewModel.events.value
        assertTrue("The UI must be told the session is offline", event is AuthEvent.SignedInOffline)
        assertEquals("Unable to resolve host", (event as AuthEvent.SignedInOffline).error.message)

        viewModel.consumeEvent()
        assertNull(viewModel.events.value)
    }

    @Test
    fun authViewModel_offlineSessionOnDisk_countsAsExistingSession() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        whenever(tokenStore.isOfflineSession).thenReturn(true)

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        assertEquals(true, viewModel.hasExistingSession.value)
    }
}
