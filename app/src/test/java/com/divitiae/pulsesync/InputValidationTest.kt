package com.divitiae.pulsesync

import com.divitiae.pulsesync.ui.auth.AuthValidation
import com.divitiae.pulsesync.ui.settings.KeywordValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for input validation logic across the application:
 * - Empty string checks
 * - Title and password length constraints
 * - Keyword and tag sanitization
 *
 * Code Attribution No 4
 * This method was taken from "Android Unit Testing Fundamentals and Input Validation Patterns"
 * https://developer.android.com/training/testing/fundamentals
 * Android Developers
 */
class InputValidationTest {

    // Keyword Validation & Sanitization Tests

    @Test
    fun keywordNormalise_trimsLeadingAndTrailingWhitespace() {
        assertEquals("internship", KeywordValidation.normalise("  internship  "))
        assertEquals("load shedding", KeywordValidation.normalise("\tload shedding\n"))
    }

    @Test
    fun keywordNormalise_convertsToLowercase() {
        assertEquals("nsfas", KeywordValidation.normalise("NSFAS"))
        assertEquals("eskom stage 2", KeywordValidation.normalise("Eskom Stage 2"))
    }

    @Test
    fun keywordValidate_returnsBlankForEmptyOrWhitespaceString() {
        val existing = listOf("bursary", "eskom")

        assertEquals(KeywordValidation.Result.Blank, KeywordValidation.validate("", existing))
        assertEquals(KeywordValidation.Result.Blank, KeywordValidation.validate("   ", existing))
        assertEquals(KeywordValidation.Result.Blank, KeywordValidation.validate("\t\n", existing))
    }

    @Test
    fun keywordValidate_returnsDuplicateForExactOrCaseInsensitiveMatch() {
        val existing = listOf("bursaries", "load shedding", "internships")

        // Exact match
        assertEquals(KeywordValidation.Result.Duplicate, KeywordValidation.validate("bursaries", existing))
        // Case-insensitive match
        assertEquals(KeywordValidation.Result.Duplicate, KeywordValidation.validate("BURSARIES", existing))
        // Match with leading/trailing whitespace
        assertEquals(KeywordValidation.Result.Duplicate, KeywordValidation.validate("  Load Shedding  ", existing))
    }

    @Test
    fun keywordValidate_returnsValidWithSanitizedKeywordForNewInput() {
        val existing = listOf("bursaries", "load shedding")

        val result = KeywordValidation.validate("  Vodacom Graduate  ", existing)
        assertTrue(result is KeywordValidation.Result.Valid)
        assertEquals("vodacom graduate", (result as KeywordValidation.Result.Valid).keyword)
    }

    // Credential Validation Tests (Password Length Constraints)

    @Test
    fun passwordValidation_rejectsEmptyOrShortPasswords() {
        assertFalse("Empty password must fail validation", AuthValidation.isPasswordLongEnough(""))
        assertFalse("Short password (4 chars) must fail validation", AuthValidation.isPasswordLongEnough("abcd"))
        assertFalse("Password with 7 chars must fail (min length is 8)", AuthValidation.isPasswordLongEnough("1234567"))
    }

    @Test
    fun passwordValidation_acceptsPasswordsMeetingMinimumLength() {
        assertTrue("Password with exactly 8 chars must pass", AuthValidation.isPasswordLongEnough("12345678"))
        assertTrue("Password with 16 chars must pass", AuthValidation.isPasswordLongEnough("SecurePassword123!"))
    }

    // Title & Content Constraints (Note Editor & Articles)

    @Test
    fun noteTitleValidation_detectsEmptyAndBlankStrings() {
        val emptyTitle = ""
        val whitespaceTitle = "   \t   "
        val validTitle = "My Research Notes"

        assertTrue("Empty title must be blank", emptyTitle.isBlank())
        assertTrue("Whitespace title must be blank", whitespaceTitle.isBlank())
        assertFalse("Real title must not be blank", validTitle.isBlank())
    }

    @Test
    fun noteTitleLengthConstraints_verifiesBoundaries() {
        val maxTitleLength = 100

        val validTitle = "Application Deadline Summary"
        assertTrue(validTitle.length in 1..maxTitleLength)

        val boundaryTitle = "A".repeat(100)
        assertEquals(100, boundaryTitle.length)
        assertTrue(boundaryTitle.length <= maxTitleLength)

        val overlongTitle = "A".repeat(101)
        assertTrue("Title exceeding 100 characters must exceed boundary limit", overlongTitle.length > maxTitleLength)
    }

    @Test
    fun tagSanitization_detectsBlankAndDuplicateEntries() {
        val existingTags = listOf("nsfas", "bursary", "finance")

        // Blank tag checks
        val blankTag = "   ".trim()
        assertTrue("Trimmed whitespace tag must be empty", blankTag.isEmpty())

        // Duplicate checks (case-insensitive)
        val candidateTag = "  NSFAS  ".trim()
        val isDuplicate = existingTags.any { it.equals(candidateTag, ignoreCase = true) }
        assertTrue("Candidate tag 'NSFAS' must match existing tag 'nsfas'", isDuplicate)

        // Valid new tag
        val newTag = "  education  ".trim()
        val isNewDuplicate = existingTags.any { it.equals(newTag, ignoreCase = true) }
        assertFalse("New tag 'education' must not be a duplicate", isNewDuplicate)
    }
}
