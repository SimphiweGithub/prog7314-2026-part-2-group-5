package com.divitiae.pulsesync.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.divitiae.pulsesync.ui.feed.ResourceLinkUi
import com.divitiae.pulsesync.ui.feed.ResourceType

/**
 *
 * User Defined Feature 2: binds extracted resource links to explicit
 * `ACTION_VIEW` intents so the system's browser / PDF viewer / video player
 * takes over. "Safely" means:
 *
 *  - only `http`/`https` URLs are ever launched (a malicious article body
 *    cannot smuggle `intent:`, `file:`, `javascript:` or `tel:` schemes in);
 *  - PDFs are typed as `application/pdf` first and fall back to a plain
 *    browser view when no viewer is installed;
 *  - every launch is wrapped so [ActivityNotFoundException] and
 *    [SecurityException] become a `false` return the UI turns into a Snackbar.
 *
 * @return true when an activity accepted the intent.
 */
object ExternalLinks {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    fun open(context: Context, resource: ResourceLinkUi): Boolean =
        open(context, resource.url, resource.type)

    fun open(context: Context, url: String, type: ResourceType = ResourceType.WEB): Boolean {
        val uri = parseSafely(url) ?: return false

        val intent = when (type) {
            ResourceType.PDF -> Intent(Intent.ACTION_VIEW).setDataAndType(uri, MIME_PDF)
            ResourceType.VIDEO, ResourceType.PORTAL, ResourceType.WEB ->
                Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return launch(context, intent) ||
                // A PDF with no dedicated viewer still opens fine in a browser.
                (type == ResourceType.PDF && launch(context, browserIntent(uri)))
    }

    /** True when [url] is something we are willing to hand to another app. */
    fun isSafeUrl(url: String): Boolean = parseSafely(url) != null

    private fun parseSafely(url: String): Uri? {
        val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme !in ALLOWED_SCHEMES) return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }

    private fun browserIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        // Target activity exists but refuses external callers.
        false
    }

    private const val MIME_PDF = "application/pdf"
}