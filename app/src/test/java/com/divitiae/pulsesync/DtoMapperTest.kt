package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.ResourceKind
import com.divitiae.pulsesync.data.domain.SentimentType
import com.divitiae.pulsesync.data.mapper.toCreateRequest
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toResourceKind
import com.divitiae.pulsesync.data.mapper.toSentimentType
import com.divitiae.pulsesync.data.mapper.toUpdateRequest
import com.divitiae.pulsesync.data.remote.dto.ArticleDto
import com.divitiae.pulsesync.data.remote.dto.CategoryDto
import com.divitiae.pulsesync.data.remote.dto.FullTextDto
import com.divitiae.pulsesync.data.remote.dto.KeywordDto
import com.divitiae.pulsesync.data.remote.dto.NoteDto
import com.divitiae.pulsesync.data.remote.dto.NotificationDto
import com.divitiae.pulsesync.data.remote.dto.UserDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DTO-to-Domain mappers and Gson JSON deserialization
 * against the Part 1 REST API contracts.
 *
 * Code Attribution No 3
 * This method was taken from "Parse JSON with Gson in Android and Kotlin"
 * https://github.com/google/gson/blob/master/UserGuide.md#TOC-Overview
 * Google
 */
class DtoMapperTest {

    private val gson = Gson()

    // ArticleDto Deserialization & Mapping

    @Test
    fun articleDto_gsonDeserializationAndDomainMapping() {
        val articleJson = """
            {
              "articleId": "nsfas-2027",
              "headline": "NSFAS Opens 2027 Bursary Applications for First-Year Students",
              "sourceName": "News24",
              "sourceUrl": "https://www.news24.com/article/1",
              "author": "Sipho Dlamini",
              "category": "bursaries",
              "imageUrl": "https://example.com/nsfas.jpg",
              "publishedAt": 1786195200000,
              "readTimeMinutes": 4,
              "aiSummary": {
                "detailed": [
                  "NSFAS has officially opened applications for the 2027 academic year.",
                  "Applicants need certified ID copies and proof of income."
                ],
                "condensed": [
                  "NSFAS 2027 applications are open.",
                  "Certified documents required."
                ],
                "model": "gemini-2.5-flash",
                "generatedAt": 1786195250000
              },
              "sentiment": {
                "score": 0.85,
                "label": "positive"
              },
              "derivedTags": ["nsfas", "bursary", "education"],
              "extractedLinks": [
                {
                  "url": "https://nsfas.org.za/form.pdf",
                  "label": "Bursary Application Form (PDF)",
                  "linkType": "PDF"
                },
                {
                  "url": "https://my.nsfas.org.za/",
                  "label": "Official Portal",
                  "linkType": "PORTAL"
                }
              ]
            }
        """.trimIndent()

        val dto = gson.fromJson(articleJson, ArticleDto::class.java)
        val domain = dto.toDomain()

        assertEquals("nsfas-2027", domain.id)
        assertEquals("NSFAS Opens 2027 Bursary Applications for First-Year Students", domain.headline)
        assertEquals("News24", domain.sourceName)
        assertEquals("https://www.news24.com/article/1", domain.sourceUrl)
        assertEquals("Sipho Dlamini", domain.author)
        assertEquals("bursaries", domain.category)
        assertEquals("https://example.com/nsfas.jpg", domain.imageUrl)
        assertEquals(1786195200000L, domain.publishedAt)
        assertEquals(4, domain.readTimeMinutes)

        // Nested AI summary mapping
        assertEquals(2, domain.summary.detailed.size)
        assertEquals(2, domain.summary.condensed.size)
        assertEquals("gemini-2.5-flash", domain.summary.model)

        // Sentiment mapping
        assertEquals(SentimentType.POSITIVE, domain.sentiment)

        // Tags
        assertEquals(listOf("nsfas", "bursary", "education"), domain.tags)

        // Extracted resources mapping
        assertEquals(2, domain.resources.size)
        assertEquals(ResourceKind.PDF, domain.resources[0].kind)
        assertEquals("Bursary Application Form (PDF)", domain.resources[0].label)
        assertEquals(ResourceKind.PORTAL, domain.resources[1].kind)
    }

    // NoteDto Deserialization & Mapping

    @Test
    fun noteDto_gsonDeserializationAndDomainMapping() {
        val noteJson = """
            {
              "noteId": "server-note-42",
              "localId": "local-uuid-101",
              "articleId": "nsfas-2027",
              "title": "Document Checklist",
              "bodyHtml": "<p>Need ID & proof of income</p>",
              "plainText": "Need ID & proof of income",
              "tags": ["urgent", "application"],
              "isVaultNote": false,
              "isPinned": true,
              "createdAt": 1786196000000,
              "updatedAt": 1786196100000
            }
        """.trimIndent()

        val dto = gson.fromJson(noteJson, NoteDto::class.java)
        val domain = dto.toDomain()

        assertEquals("local-uuid-101", domain.localId)
        assertEquals("server-note-42", domain.serverId)
        assertEquals("nsfas-2027", domain.articleId)
        assertEquals("Document Checklist", domain.title)
        assertEquals("<p>Need ID & proof of income</p>", domain.bodyHtml)
        assertEquals("Need ID & proof of income", domain.plainText)
        assertEquals(listOf("urgent", "application"), domain.tags)
        assertFalse(domain.isVaultNote)
        assertTrue(domain.isPinned)
        assertTrue(domain.isSynced) // Coming from server DTO implies synced
        assertEquals(1786196000000L, domain.createdAt)
        assertEquals(1786196100000L, domain.updatedAt)
    }

    @Test
    fun noteDto_whenLocalIdIsNull_fallsBackToNoteId() {
        val dto = NoteDto(
            noteId = "server-only-id",
            localId = null,
            title = "Test Note",
        )
        val domain = dto.toDomain()
        assertEquals("server-only-id", domain.localId)
        assertEquals("server-only-id", domain.serverId)
    }

    @Test
    fun domainNote_mapsToCreateAndUpdateRequestDtos() {
        val note = Note(
            localId = "local-123",
            serverId = "server-456",
            articleId = "article-1",
            title = "Research Strategy",
            bodyHtml = "<p>Step 1</p>",
            plainText = "Step 1",
            tags = listOf("planning"),
            isVaultNote = false,
            isPinned = true,
            isSynced = true,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val createRequest = note.toCreateRequest()
        assertEquals("local-123", createRequest.localId)
        assertEquals("Research Strategy", createRequest.title)
        assertEquals("<p>Step 1</p>", createRequest.bodyHtml)
        assertEquals(2000L, createRequest.updatedAt)

        val updateRequest = note.toUpdateRequest()
        assertEquals("Research Strategy", updateRequest.title)
        assertTrue(updateRequest.isPinned)
        assertEquals(listOf("planning"), updateRequest.tags)
        assertEquals(2000L, updateRequest.updatedAt)
    }

    // CategoryDto & KeywordDto Deserialization

    @Test
    fun categoryDto_gsonDeserializationAndDomainMapping() {
        val categoryJson = """
            {
              "slug": "load-shedding",
              "nameEn": "Load Shedding",
              "nameZu": "Ukucinywa kukagesi",
              "nameAf": "Beurtkrag",
              "isSubscribed": true
            }
        """.trimIndent()

        val dto = gson.fromJson(categoryJson, CategoryDto::class.java)
        val domain = dto.toDomain()

        assertEquals("load-shedding", domain.slug)
        assertEquals("Load Shedding", domain.nameEn)
        assertEquals("Ukucinywa kukagesi", domain.nameZu)
        assertEquals("Beurtkrag", domain.nameAf)
        assertTrue(domain.isSubscribed)
    }

    @Test
    fun keywordDto_gsonDeserializationAndDomainMapping() {
        val keywordJson = """
            {
              "keywordId": "kw-001",
              "keyword": "bursaries",
              "notifyOnMatch": true
            }
        """.trimIndent()

        val dto = gson.fromJson(keywordJson, KeywordDto::class.java)
        val domain = dto.toDomain()

        assertEquals("kw-001", domain.id)
        assertEquals("bursaries", domain.keyword)
        assertTrue(domain.notifyOnMatch)
    }

    // NotificationDto, UserDto, and FullTextDto Deserialization

    @Test
    fun notificationDto_gsonDeserializationAndDomainMapping() {
        val json = """
            {
              "notificationId": "notif-99",
              "title": "Bursary Alert",
              "body": "NSFAS 2027 application period closes next week.",
              "type": "ALERT",
              "articleId": "nsfas-2027",
              "matchedKeyword": "bursaries",
              "isRead": false,
              "sentAt": 1786200000000
            }
        """.trimIndent()

        val dto = gson.fromJson(json, NotificationDto::class.java)
        val domain = dto.toDomain()

        assertEquals("notif-99", domain.id)
        assertEquals("Bursary Alert", domain.title)
        assertEquals("ALERT", domain.type)
        assertEquals("nsfas-2027", domain.articleId)
        assertEquals("bursaries", domain.matchedKeyword)
        assertFalse(domain.isRead)
        assertEquals(1786200000000L, domain.receivedAt)
    }

    @Test
    fun userDto_gsonDeserializationAndDomainMapping() {
        val json = """
            {
              "userId": "usr-01",
              "email": "student@dut4life.ac.za",
              "displayName": "Zanele Khumalo",
              "photoUrl": "https://example.com/profile.png",
              "preferredLanguage": "zu",
              "biometricEnabled": true
            }
        """.trimIndent()

        val dto = gson.fromJson(json, UserDto::class.java)
        val domain = dto.toDomain()

        assertEquals("usr-01", domain.userId)
        assertEquals("student@dut4life.ac.za", domain.email)
        assertEquals("Zanele Khumalo", domain.displayName)
        assertEquals("zu", domain.preferredLanguage)
        assertTrue(domain.biometricEnabled)
    }

    @Test
    fun fullTextDto_gsonDeserializationAndDomainMapping() {
        val json = """
            {
              "articleId": "nsfas-2027",
              "sanitizedText": "Full article sanitized text here...",
              "wordCount": 850,
              "sizeBytes": 5240,
              "retrievedAt": 1786195200000
            }
        """.trimIndent()

        val dto = gson.fromJson(json, FullTextDto::class.java)
        val domain = dto.toDomain()

        assertEquals("nsfas-2027", domain.articleId)
        assertEquals("Full article sanitized text here...", domain.sanitizedText)
        assertEquals(850, domain.wordCount)
        assertEquals(5240, domain.sizeBytes)
    }

    // Classification Helpers (SentimentType and ResourceKind)

    @Test
    fun sentimentTypeMapper_handlesAllVariantsAndFallbacks() {
        assertEquals(SentimentType.POSITIVE, "positive".toSentimentType())
        assertEquals(SentimentType.POSITIVE, "POSITIVE".toSentimentType())
        assertEquals(SentimentType.NEGATIVE, "negative".toSentimentType())
        assertEquals(SentimentType.NEGATIVE, "NEGATIVE".toSentimentType())
        assertEquals(SentimentType.NEUTRAL, "neutral".toSentimentType())
        assertEquals(SentimentType.NEUTRAL, "unrecognized".toSentimentType())
        assertEquals(SentimentType.NEUTRAL, (null as String?).toSentimentType())
    }

    @Test
    fun resourceKindMapper_handlesAllClassifications() {
        assertEquals(ResourceKind.PDF, "PDF".toResourceKind())
        assertEquals(ResourceKind.PDF, "pdf_form".toResourceKind())
        assertEquals(ResourceKind.PORTAL, "portal".toResourceKind())
        assertEquals(ResourceKind.PORTAL, "APPLICATION_PORTAL".toResourceKind())
        assertEquals(ResourceKind.PORTAL, "bursary".toResourceKind())
        assertEquals(ResourceKind.VIDEO, "video".toResourceKind())
        assertEquals(ResourceKind.WEB, "web".toResourceKind())
        assertEquals(ResourceKind.WEB, "unknown_link".toResourceKind())
        assertEquals(ResourceKind.WEB, (null as String?).toResourceKind())
    }
}
