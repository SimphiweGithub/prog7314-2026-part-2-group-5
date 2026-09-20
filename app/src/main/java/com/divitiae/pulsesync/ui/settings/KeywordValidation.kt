package com.divitiae.pulsesync.ui.settings

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The sealed-interface result type and data objects in this file follow the Kotlin conventions documented in:
 *
 * JetBrains (n.d.) Sealed classes and interfaces. [online]
 * Available at: https://kotlinlang.org/docs/sealed-classes.html
 * [Accessed 20 September 2026].
 *
 * JetBrains (n.d.) Object declarations and expressions. [online]
 * Available at: https://kotlinlang.org/docs/object-declarations.html
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

/**
 * Client-side checks for the tracking-keyword input in Settings. Mirrors the
 * pattern in AuthValidation: the UI decides how to surface a rejection, this
 * only decides whether (and why) an entry is rejected.
 */
internal object KeywordValidation {

    // Adapted from: JetBrains (n.d.) Sealed classes and interfaces. https://kotlinlang.org/docs/sealed-classes.html
    sealed interface Result {
        /** Keyword is acceptable; [keyword] is the normalised value to store. */
        data class Valid(val keyword: String) : Result

        /** Draft was blank or whitespace-only. */
        data object Blank : Result

        /** Keyword (case-insensitive) is already being tracked. */
        data object Duplicate : Result
    }

    /** Trim and lowercase so "Internship " and "internship" are the same keyword. */
    fun normalise(draft: String): String = draft.trim().lowercase()

    fun validate(draft: String, existing: Collection<String>): Result {
        val keyword = normalise(draft)
        return when {
            keyword.isEmpty() -> Result.Blank
            existing.any { normalise(it) == keyword } -> Result.Duplicate
            else -> Result.Valid(keyword)
        }
    }
}
