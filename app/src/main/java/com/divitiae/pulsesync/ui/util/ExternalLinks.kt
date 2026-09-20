package com.divitiae.pulsesync.ui.util

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The explicit ACTION_VIEW intent construction, MIME-typed data and ActivityNotFoundException handling in this file were adapted from:
 *
 * Android Developers (2026) Common intents. [online]
 * Available at: https://developer.android.com/guide/components/intents-common
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Intents and intent filters. [online]
 * Available at: https://developer.android.com/guide/components/intents-filters
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) ActivityNotFoundException. [online]
 * Available at: https://developer.android.com/reference/android/content/ActivityNotFoundException
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

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
        // Adapted from: Android Developers (2026) Common intents - Load a web URL. https://developer.android.com/guide/components/intents-common
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

    // Adapted from: Android Developers (2026) ActivityNotFoundException. https://developer.android.com/reference/android/content/ActivityNotFoundException
    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
