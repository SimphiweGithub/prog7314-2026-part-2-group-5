package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.auth.AuthRepository
import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.auth.SessionManager
import com.divitiae.pulsesync.data.remote.AuthInterceptor
import com.divitiae.pulsesync.testutil.MainDispatcherRule
import com.divitiae.pulsesync.ui.auth.AuthEvent
import com.divitiae.pulsesync.ui.auth.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Client-side handling of a rejected session (backend now enforces
 * `[Authorize]` on every per-user endpoint, so a stale or forged token yields
 * HTTP 401 instead of silently reading "demo-user" data).
 *
 * Covers the three layers involved: the OkHttp interceptor that detects the
 * 401, the [SessionManager] that wipes the token exactly once, and the
 * [AuthViewModel] that drops the "existing session" shortcut and queues the
 * "session expired" notice for the Sign In screen.
 *
 * Code Attribution No 48
 * This method was taken from "MockWebServer: scriptable web server for testing HTTP clients"
 * https://github.com/square/okhttp/tree/master/mockwebserver
 * Square, Inc.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---- AuthInterceptor ----------------------------------------------------------------

    private val server = MockWebServer()

    @Before
    fun startServer() = server.start()

    @After
    fun stopServer() = server.shutdown()

    private fun get(path: String, token: String?, onUnauthorized: () -> Unit): Int {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider = { token }, languageProvider = { "en" }, onUnauthorized = onUnauthorized))
            .build()
        return client.newCall(Request.Builder().url(server.url(path)).build()).execute().use { it.code }
    }

    @Test
    fun interceptor_401WithBearerToken_expiresSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        var calls = 0

        val code = get("/api/v1/notes", token = "stale.jwt", onUnauthorized = { calls++ })

        assertEquals(401, code)
        assertEquals("Bearer stale.jwt", server.takeRequest().getHeader("Authorization"))
        assertEquals(1, calls)
    }

    @Test
    fun interceptor_401WithoutToken_isNotASessionExpiry() {
        server.enqueue(MockResponse().setResponseCode(401))
        var calls = 0

        get("/api/v1/notes", token = null, onUnauthorized = { calls++ })

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, calls)
    }

    @Test
    fun interceptor_401OnAuthEndpoint_isNotASessionExpiry() {
        server.enqueue(MockResponse().setResponseCode(401))
        var calls = 0

        get("/api/v1/auth/logout", token = "stale.jwt", onUnauthorized = { calls++ })

        assertEquals(0, calls)
    }

    @Test
    fun interceptor_successWithToken_leavesSessionAlone() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        var calls = 0

        val code = get("/api/v1/notes", token = "good.jwt", onUnauthorized = { calls++ })

        assertEquals(200, code)
        assertEquals(0, calls)
    }

    // ---- SessionManager -----------------------------------------------------------------

    @Test
    fun sessionManager_clearsTokensOnceAndRaisesFlag() = runTest {
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn("stale.jwt")
        val manager = SessionManager(tokenStore, scope = this)

        // Three parallel requests all coming back 401. The manager launches its
        // clear() on the TestScope itself so advanceUntilIdle() drives it (background
        // tasks are skipped once the foreground is idle).
        manager.onUnauthorized()
        manager.onUnauthorized()
        manager.onUnauthorized()
        advanceUntilIdle()

        assertTrue(manager.sessionExpired.value)
        verify(tokenStore, times(1)).clear()

        manager.acknowledgeExpiry()
        assertFalse(manager.sessionExpired.value)
    }

    @Test
    fun sessionManager_withNoStoredToken_doesNothing() = runTest {
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn(null)
        val manager = SessionManager(tokenStore, scope = this)

        manager.onUnauthorized()
        advanceUntilIdle()

        assertFalse(manager.sessionExpired.value)
        verify(tokenStore, never()).clear()
    }

    // ---- AuthViewModel ------------------------------------------------------------------

    @Test
    fun authViewModel_sessionExpiry_dropsExistingSessionAndQueuesNotice() = runTest {
        val authRepository: AuthRepository = mock()
        val tokenStore: AuthTokenStore = mock()
        whenever(tokenStore.accessToken).thenReturn("stored.jwt")
        val manager = SessionManager(tokenStore, scope = this)
        val viewModel = AuthViewModel(authRepository, tokenStore, manager)
        advanceUntilIdle()
        assertEquals(true, viewModel.hasExistingSession.value)

        manager.onUnauthorized()
        advanceUntilIdle()

        assertTrue("NavHost must be told to route to Sign In", viewModel.sessionExpired.value)
        assertEquals(false, viewModel.hasExistingSession.value)
        assertNull(viewModel.signInState.value)

        viewModel.onSessionExpiryHandled()
        advanceUntilIdle()

        assertFalse("Flag resets once Sign In is showing", viewModel.sessionExpired.value)
        assertEquals(AuthEvent.SessionExpired, viewModel.events.value)
        viewModel.consumeEvent()
        assertNull(viewModel.events.value)
    }
}
