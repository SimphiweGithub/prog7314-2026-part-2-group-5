package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.auth.TokenAuthenticator
import com.divitiae.pulsesync.data.remote.AuthInterceptor
import com.divitiae.pulsesync.data.remote.dto.AuthResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Silent refresh through the real OkHttp pipeline (MockWebServer): the
 * [TokenAuthenticator] must replay a 401'd request with a freshly minted
 * access token, and only when the refresh itself is rejected may the 401
 * reach [AuthInterceptor] and expire the session.
 *
 * Code Attribution No 51
 * This method was taken from "MockWebServer: scriptable web server for testing HTTP clients"
 * https://github.com/square/okhttp/tree/master/mockwebserver
 * Square, Inc.
 */
class TokenAuthenticatorTest {

    private val server = MockWebServer()
    private val tokenStore: AuthTokenStore = mock()
    private var expiredCalls = 0
    private var refreshCalls = mutableListOf<String>()

    @Before
    fun startServer() = server.start()

    @After
    fun stopServer() = server.shutdown()

    private fun client(
        token: String?,
        refreshResult: AuthResponseDto?,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor({ token }, { "en" }, onUnauthorized = { expiredCalls++ }))
        .authenticator(TokenAuthenticator(tokenStore) { rt -> refreshCalls += rt; refreshResult })
        .build()

    private fun OkHttpClient.get(path: String = "/api/v1/notes"): Int =
        newCall(Request.Builder().url(server.url(path)).build()).execute().use { it.code }

    @Test
    fun expiredAccessToken_isRefreshedAndRequestReplayed() = runBlocking {
        whenever(tokenStore.accessToken).thenReturn("old.jwt")
        whenever(tokenStore.refreshToken()).thenReturn("refresh-1")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val code = client(token = "old.jwt", refreshResult = AuthResponseDto(userId = "u1", accessToken = "new.jwt", refreshToken = "refresh-2")).get()

        assertEquals(200, code)
        assertEquals("Bearer old.jwt", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new.jwt", server.takeRequest().getHeader("Authorization"))
        assertEquals(listOf("refresh-1"), refreshCalls)
        verify(tokenStore).save("new.jwt", "refresh-2")
        assertEquals("Refresh succeeded, so the session must not be expired", 0, expiredCalls)
    }

    @Test
    fun rejectedRefresh_letsThe401ThroughAndExpiresSession() = runBlocking {
        whenever(tokenStore.accessToken).thenReturn("old.jwt")
        whenever(tokenStore.refreshToken()).thenReturn("refresh-revoked")
        server.enqueue(MockResponse().setResponseCode(401))

        val code = client(token = "old.jwt", refreshResult = null).get()

        assertEquals(401, code)
        assertEquals(1, server.requestCount)
        assertEquals(listOf("refresh-revoked"), refreshCalls)
        verify(tokenStore, never()).save(any(), any())
        assertEquals(1, expiredCalls)
    }

    @Test
    fun requestWithoutToken_isNeverRefreshed() = runBlocking {
        whenever(tokenStore.accessToken).thenReturn(null)
        server.enqueue(MockResponse().setResponseCode(401))

        val code = client(token = null, refreshResult = AuthResponseDto(accessToken = "unused")).get()

        assertEquals(401, code)
        assertEquals(emptyList<String>(), refreshCalls)
        assertEquals(0, expiredCalls)
    }

    @Test
    fun tokenAlreadyRotatedByAnotherRequest_isReusedWithoutASecondRefresh() = runBlocking {
        // The request went out with the old token, but by the time the 401
        // arrives another thread has already stored a new one.
        whenever(tokenStore.accessToken).thenReturn("rotated.jwt")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val code = client(token = "old.jwt", refreshResult = null).get()

        assertEquals(200, code)
        server.takeRequest()
        assertEquals("Bearer rotated.jwt", server.takeRequest().getHeader("Authorization"))
        assertEquals(emptyList<String>(), refreshCalls)
        assertNull(server.takeRequest(0, java.util.concurrent.TimeUnit.MILLISECONDS))
        assertEquals(0, expiredCalls)
    }

    @Test
    fun replayedRequestRejectedAgain_givesUpAfterOneRetry() = runBlocking {
        whenever(tokenStore.accessToken).thenReturn("old.jwt")
        whenever(tokenStore.refreshToken()).thenReturn("refresh-1")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val code = client(token = "old.jwt", refreshResult = AuthResponseDto(accessToken = "new.jwt", refreshToken = "refresh-2")).get()

        assertEquals(401, code)
        assertEquals(2, server.requestCount)
        assertEquals(listOf("refresh-1"), refreshCalls)
        assertEquals(1, expiredCalls)
    }
}
