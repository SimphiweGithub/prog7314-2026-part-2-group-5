package com.divitiae.pulsesync.ui.settings

/**
 * Code Attribution No 28
 * This method was taken from "Sealed classes and interfaces in Kotlin"
 * https://kotlinlang.org/docs/sealed-classes.html
 * JetBrains
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
