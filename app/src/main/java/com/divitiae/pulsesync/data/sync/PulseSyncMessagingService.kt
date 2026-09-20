package com.divitiae.pulsesync.data.sync

import com.divitiae.pulsesync.PulseSyncApplication
import com.divitiae.pulsesync.data.domain.NotificationItem
import com.divitiae.pulsesync.data.remote.dto.FcmTokenRequestDto
import com.divitiae.pulsesync.data.repository.safeApiCallEmpty
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives keyword-match pushes. Persists each one through the notification
 * repository so it appears in the in-app alerts list, and registers refreshed
 * FCM tokens with the backend.
 *
 * Requires google-services.json to actually receive messages; until then this
 * simply never fires.
 */
class PulseSyncMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as? PulseSyncApplication)?.container ?: return
        val data = message.data
        val item = NotificationItem(
            id = data["notificationId"] ?: UUID.randomUUID().toString(),
            title = message.notification?.title ?: data["title"] ?: "PulseSync",
            body = message.notification?.body ?: data["body"].orEmpty(),
            type = data["type"] ?: "INFO",
            articleId = data["articleId"],
            matchedKeyword = data["matchedKeyword"],
            isRead = false,
            receivedAt = System.currentTimeMillis(),
        )
        scope.launch { container.notificationRepository.record(item) }
    }

    override fun onNewToken(token: String) {
        val container = (application as? PulseSyncApplication)?.container ?: return
        scope.launch {
            safeApiCallEmpty { container.api.registerFcmToken(FcmTokenRequestDto(token)) }
        }
    }
}
