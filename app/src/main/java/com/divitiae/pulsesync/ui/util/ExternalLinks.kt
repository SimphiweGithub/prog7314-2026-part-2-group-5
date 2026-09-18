package com.divitiae.pulsesync.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.divitiae.pulsesync.ui.feed.ResourceType

/**
 * Explicit-intent helpers for User Defined Feature 2 (Extracted Resource
 * Links Panel). Each link is handed to the system with ACTION_VIEW so the
 * right app (browser, PDF viewer, video player) takes over.
 *
 * @return true when an activity accepted the intent, false when nothing on
 * the device can handle it so the caller can show feedback.
 */
object ExternalLinks {

    fun open(context: Context, url: String, type: ResourceType = ResourceType.WEB): Boolean {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            when (type) {
                ResourceType.PDF -> setDataAndType(uri, "application/pdf")
                ResourceType.VIDEO, ResourceType.PORTAL, ResourceType.WEB -> data = uri
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(context, intent) ||
            // A PDF with no dedicated viewer still opens fine in a browser.
            (type == ResourceType.PDF && launch(context, Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
    }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
