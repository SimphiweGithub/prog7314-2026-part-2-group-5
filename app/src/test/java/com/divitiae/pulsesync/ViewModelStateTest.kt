package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.auth.AuthRepository
import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.auth.SessionManager
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.testutil.MainDispatcherRule
import com.divitiae.pulsesync.ui.auth.AuthEvent
import com.divitiae.pulsesync.ui.auth.AuthViewModel
import com.divitiae.pulsesync.ui.auth.GoogleSignInFailure
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.dataOrNull
import com.divitiae.pulsesync.ui.common.isLoading
import com.divitiae.pulsesync.ui.common.map
import com.divitiae.pulsesync.ui.common.toUiState
import com.divitiae.pulsesync.ui.common.uiStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.repository.ArticleRepository
import com.divitiae.pulsesync.data.repository.CategoryRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.ui.feed.FeedViewModel
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.times
import java.io.IOException

/**
 * ViewModel state and UI state transition tests (Member 4 — Robustness & Error Boundaries).
 *
 * Verifies:
 *  1. [UiState] state contract: Loading, Success, Error, and mapping primitives.
 *  2. [uiStateOf] exception-to-state boundaries: IOException -> Network, RuntimeException -> Unknown,
 *     and CancellationException preservation.
 *  3. [AuthViewModel] state transitions across Loading, Success, and Error for Google SSO,
 *     failure states, debouncing, error consumption, and sign-out.
 *
 * Code Attribution No 2
 * This method was taken from "Testing ViewModels with StateFlow and runTest"
 * https://developer.android.com/topic/architecture/ui-layer/state-production#testing
 * Android Developers
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleUser = UserProfile(
        userId = "usr-test-123",
        email = "test.user@pulsesync.co.za",
        displayName = "Test User",
        photoUrl = "https://pulsesync.co.za/avatars/usr-test-123.png",
    )

    // UiState Primitives & Mapping Tests

    @Test
    fun uiState_loadingStateContract() {
        val state: UiState<String> = UiState.Loading

        assertTrue("UiState.Loading must indicate isLoading", state.isLoading)
        assertNull("UiState.Loading must return null data", state.dataOrNull())
    }

    @Test
    fun uiState_successStateContract() {
        val state: UiState<String> = UiState.Success("payload")

        assertFalse("UiState.Success must not indicate isLoading", state.isLoading)
        assertEquals("payload", state.dataOrNull())
        assertEquals("payload", (state as UiState.Success).data)
    }

    @Test
    fun uiState_errorStateContract_unauthorizedIsNotRetryable() {
        val error = AppError.Unauthorized("Token expired")
        val state: UiState.Error = error.toUiState()

        assertEquals("Token expired", state.message)
        assertEquals(error, state.error)
        assertFalse("Unauthorized errors must not be retryable", state.retryable)
        assertFalse("Error state must not be loading", state.isLoading)
        assertNull("Error state data must be null", state.dataOrNull())
    }

    @Test
    fun uiState_errorStateContract_networkIsRetryable() {
        val error = AppError.Network("DNS resolution failed")
        val state: UiState.Error = error.toUiState()

        assertEquals("DNS resolution failed", state.message)
        assertEquals(error, state.error)
        assertTrue("Network errors must be retryable", state.retryable)
    }

    @Test
    fun uiState_errorStateContract_httpIsRetryable() {
        val error = AppError.Http(500, "Internal Server Error")
        val state: UiState.Error = error.toUiState()

        assertEquals("Internal Server Error", state.message)
        assertEquals(500, (state.error as AppError.Http).code)
        assertTrue("Server HTTP errors should be retryable", state.retryable)
    }

    @Test
    fun uiState_errorStateContract_unknownIsRetryable() {
        val error = AppError.Unknown("Unexpected parser failure")
        val state: UiState.Error = error.toUiState()

        assertEquals("Unexpected parser failure", state.message)
        assertTrue("Unknown errors are retryable by default", state.retryable)
    }

    @Test
    fun uiState_resultToUiStateBridge() {
        val successResult: Result<String> = Result.Success("article-content")
        val successState = successResult.toUiState()
        assertTrue(successState is UiState.Success)
        assertEquals("article-content", (successState as UiState.Success).data)

        val failureResult: Result<String> = Result.Failure(AppError.Network("Timeout"))
        val errorState = failureResult.toUiState()
        assertTrue(errorState is UiState.Error)
        assertEquals("Timeout", (errorState as UiState.Error).message)
        assertTrue(errorState.retryable)
    }

    @Test
    fun uiState_mapTransformations() {
        val successState: UiState<Int> = UiState.Success(42)
        val mappedSuccess = successState.map { it * 2 }
        assertEquals(84, mappedSuccess.dataOrNull())

        val loadingState: UiState<Int> = UiState.Loading
        val mappedLoading = loadingState.map { it * 2 }
        assertTrue(mappedLoading is UiState.Loading)

        val errorState: UiState<Int> = UiState.Error("failed", AppError.Unknown("unknown"))
        val mappedError = errorState.map { it * 2 }
        assertTrue(mappedError is UiState.Error)
        assertEquals("failed", (mappedError as UiState.Error).message)
    }

    @Test
    fun uiStateOf_catchesIOExceptionAsNetworkError() = runTest {
        val state = uiStateOf<String> {
            throw IOException("Connection reset by peer")
        }

        assertTrue("uiStateOf must convert IOException to UiState.Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertTrue("Underlying error must be AppError.Network", errorState.error is AppError.Network)
        assertTrue("Network error must be retryable", errorState.retryable)
        assertEquals("Connection reset by peer", errorState.message)
    }

    @Test
    fun uiStateOf_catchesGenericExceptionAsUnknownError() = runTest {
        val state = uiStateOf<String> {
            throw IllegalStateException("Unexpected illegal state")
        }

        assertTrue("uiStateOf must convert RuntimeException to UiState.Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertTrue("Underlying error must be AppError.Unknown", errorState.error is AppError.Unknown)
        assertEquals("Unexpected illegal state", errorState.message)
    }

    @Test(expected = CancellationException::class)
    fun uiStateOf_neverSwallowsCancellationException() = runTest {
        uiStateOf<String> {
            throw CancellationException("Coroutine scope cancelled")
        }
    }

    @Test
    fun uiStateOf_returnsSuccessOnNormalCompletion() = runTest {
        val state = uiStateOf { "computed-result" }
        assertTrue(state is UiState.Success)
        assertEquals("computed-result", (state as UiState.Success).data)
    }

    // AuthViewModel State Transitions (Loading, Success, Error)

    @Test
    fun authViewModel_initialStateWithoutSession() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        verify(tokenStore).load()
        assertEquals(false, viewModel.hasExistingSession.value)
        assertNull("Initial sign-in state must be idle (null)", viewModel.signInState.value)
        assertNull("Initial auth events must be null", viewModel.events.value)
    }

    @Test
    fun authViewModel_initialStateWithExistingSession() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn("stored.jwt.token")

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        verify(tokenStore).load()
        assertEquals(true, viewModel.hasExistingSession.value)
        assertNull(viewModel.signInState.value)
    }

    @Test
    fun authViewModel_googleSignIn_transitionsToSuccess() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        whenever(authRepository.signInWithGoogleIdToken(eq("google-id-token-valid"), anyOrNull()))
            .thenReturn(Result.Success(sampleUser))

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        viewModel.onGoogleIdToken("google-id-token-valid")
        advanceUntilIdle()

        val state = viewModel.signInState.value
        assertNotNull("Sign-in state must not be null after completion", state)
        assertTrue("Sign-in state must transition to Success", state is UiState.Success)
        val successState = state as UiState.Success<UserProfile>
        assertEquals("usr-test-123", successState.data.userId)
        assertEquals("test.user@pulsesync.co.za", successState.data.email)
        assertEquals("Test User", successState.data.displayName)
    }

    @Test
    fun authViewModel_googleSignIn_transitionsToErrorOnUnauthorized() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        whenever(authRepository.signInWithGoogleIdToken(eq("google-id-token-expired"), anyOrNull()))
            .thenReturn(Result.Failure(AppError.Unauthorized("Invalid or expired ID token")))

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        viewModel.onGoogleIdToken("google-id-token-expired")
        advanceUntilIdle()

        val state = viewModel.signInState.value
        assertNotNull(state)
        assertTrue("Sign-in state must transition to Error on auth failure", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals("Invalid or expired ID token", errorState.message)
        assertTrue("Underlying error should be AppError.Unauthorized", errorState.error is AppError.Unauthorized)
        assertFalse("Unauthorized error must not be retryable", errorState.retryable)
    }

    @Test
    fun authViewModel_googleSignIn_transitionsToErrorOnNetworkFailure() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        whenever(authRepository.signInWithGoogleIdToken(eq("google-id-token-net-fail"), anyOrNull()))
            .thenReturn(Result.Failure(AppError.Network("Unable to resolve host")))

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()

        viewModel.onGoogleIdToken("google-id-token-net-fail")
        advanceUntilIdle()

        val state = viewModel.signInState.value
        assertNotNull(state)
        assertTrue("Sign-in state must transition to Error on network error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals("Unable to resolve host", errorState.message)
        assertTrue("Network error must be retryable", errorState.retryable)
    }

    @Test
    fun authViewModel_onGoogleSignInFailed_noIdToken() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))

        viewModel.onGoogleSignInFailed(GoogleSignInFailure.NO_ID_TOKEN)

        val state = viewModel.signInState.value
        assertTrue("NO_ID_TOKEN must emit UiState.Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals(GoogleSignInFailure.NO_ID_TOKEN.detail, errorState.message)
        assertTrue("NO_ID_TOKEN is retryable", errorState.retryable)

        val event = viewModel.events.value
        assertTrue(event is AuthEvent.GoogleFailure)
        assertEquals(GoogleSignInFailure.NO_ID_TOKEN, (event as AuthEvent.GoogleFailure).reason)
    }

    @Test
    fun authViewModel_onGoogleSignInFailed_notConfiguredIsNotRetryable() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))

        viewModel.onGoogleSignInFailed(GoogleSignInFailure.NOT_CONFIGURED)

        val state = viewModel.signInState.value
        assertTrue("NOT_CONFIGURED must emit UiState.Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals(GoogleSignInFailure.NOT_CONFIGURED.detail, errorState.message)
        assertFalse("NOT_CONFIGURED is a build config issue and not retryable", errorState.retryable)

        val event = viewModel.events.value
        assertTrue(event is AuthEvent.GoogleFailure)
        assertEquals(GoogleSignInFailure.NOT_CONFIGURED, (event as AuthEvent.GoogleFailure).reason)
    }

    @Test
    fun authViewModel_onGoogleSignInFailed_nullCancellationRemainsIdle() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))

        viewModel.onGoogleSignInFailed(null)

        assertNull("User cancellation (null) must keep state idle", viewModel.signInState.value)
        assertNull("User cancellation must not emit failure event", viewModel.events.value)
    }

    @Test
    fun authViewModel_consumeError_clearsErrorState() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))

        viewModel.onGoogleSignInFailed(GoogleSignInFailure.API_ERROR)
        assertTrue(viewModel.signInState.value is UiState.Error)

        viewModel.consumeError()
        assertNull("consumeError must reset UiState.Error back to null", viewModel.signInState.value)
    }

    @Test
    fun authViewModel_consumeEvent_clearsEvent() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))

        viewModel.onEmailSignInRequested()
        assertEquals(AuthEvent.EmailNotAvailable, viewModel.events.value)

        viewModel.consumeEvent()
        assertNull("consumeEvent must reset events flow to null", viewModel.events.value)
    }

    @Test
    fun authViewModel_signOut_clearsSessionAndState() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn("active_token")

        val viewModel = AuthViewModel(authRepository, tokenStore, SessionManager(tokenStore, scope = this))
        advanceUntilIdle()
        assertEquals(true, viewModel.hasExistingSession.value)

        viewModel.signOut()
        advanceUntilIdle()

        verify(authRepository).signOut()
        assertNull("signOut must clear signInState", viewModel.signInState.value)
        assertEquals(false, viewModel.hasExistingSession.value)
    }

    // FeedViewModel State Transitions (Refresh, Loading, Error)

    @Test
    fun feedViewModel_refreshFailure_updatesRefreshError() = runTest {
        val articleRepository: ArticleRepository = mock()
        val categoryRepository: CategoryRepository = mock()
        val preferencesRepository: PreferencesRepository = mock()
        val downloadRepository: DownloadRepository = mock()

        whenever(articleRepository.observeFeed(anyOrNull())).thenReturn(flowOf(emptyList()))
        whenever(categoryRepository.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(preferencesRepository.preferences).thenReturn(flowOf(UserPreferences()))
        whenever(downloadRepository.observeSlots()).thenReturn(flowOf(emptyList()))
        whenever(articleRepository.refresh(anyOrNull())).thenReturn(Result.Failure(AppError.Network("Refresh timeout")))

        val viewModel = FeedViewModel(
            articleRepository = articleRepository,
            categoryRepository = categoryRepository,
            preferencesRepository = preferencesRepository,
            downloadRepository = downloadRepository,
        )
        advanceUntilIdle()

        // Trigger manual refresh
        viewModel.refresh()
        advanceUntilIdle()

        val error = viewModel.refreshError.value
        assertNotNull("Refresh failure must set refreshError", error)
        assertEquals("Refresh timeout", error?.message)
        assertTrue("Network error on refresh must be retryable", error?.retryable == true)

        viewModel.consumeRefreshError()
        assertNull("consumeRefreshError must clear the error back to null", viewModel.refreshError.value)
    }

    @Test
    fun feedViewModel_refreshSuccess_doesNotSetRefreshError() = runTest {
        val articleRepository: ArticleRepository = mock()
        val categoryRepository: CategoryRepository = mock()
        val preferencesRepository: PreferencesRepository = mock()
        val downloadRepository: DownloadRepository = mock()

        whenever(articleRepository.observeFeed(anyOrNull())).thenReturn(flowOf(emptyList()))
        whenever(categoryRepository.observeAll()).thenReturn(flowOf(emptyList()))
        whenever(preferencesRepository.preferences).thenReturn(flowOf(UserPreferences()))
        whenever(downloadRepository.observeSlots()).thenReturn(flowOf(emptyList()))
        whenever(articleRepository.refresh(anyOrNull())).thenReturn(Result.Success(Unit))

        val viewModel = FeedViewModel(
            articleRepository = articleRepository,
            categoryRepository = categoryRepository,
            preferencesRepository = preferencesRepository,
            downloadRepository = downloadRepository,
        )
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertNull("Successful refresh must not set refreshError", viewModel.refreshError.value)
    }
}
