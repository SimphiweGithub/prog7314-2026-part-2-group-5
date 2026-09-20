package com.divitiae.pulsesync.data.local.seed

import com.divitiae.pulsesync.data.domain.AiSummary
import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.ResourceKind
import com.divitiae.pulsesync.data.domain.ResourceLink
import com.divitiae.pulsesync.data.domain.SentimentType

/**
 * Offline seed content in domain form. The repository writes this into Room on
 * first launch so the app is fully usable and demoable before the REST API is
 * live. It mirrors the Figma sample stories the UI was designed against; once
 * the API is deployed, live data simply overwrites these rows.
 */
object SeedData {

    val categories: List<Category> = listOf(
        Category("bursaries", "Bursaries", "Iimali zezifundo", "Beurse", isSubscribed = true),
        Category("load-shedding", "Load Shedding", "Ukucinywa kukagesi", "Beurtkrag", isSubscribed = true),
        Category("internships", "Internships", "Izifundo zomsebenzi", "Internskappe", isSubscribed = true),
        Category("technology", "Technology", "Ubuchwepheshe", "Tegnologie", isSubscribed = false),
    )

    val articles: List<Article> = listOf(
        Article(
            id = "nsfas-2027",
            headline = "NSFAS Opens 2027 Bursary Applications for First-Year Students",
            sourceName = "News24",
            sourceUrl = "https://www.news24.com/",
            author = null,
            category = "bursaries",
            imageUrl = null,
            publishedAt = 1_786_195_200_000L,
            readTimeMinutes = 4,
            summary = AiSummary(
                detailed = listOf(
                    "NSFAS has officially opened applications for the 2027 academic year, with early " +
                        "submission strongly encouraged. Returning students must reapply through the same portal.",
                    "Applicants need a certified ID copy, proof of household income, and matric results " +
                        "uploaded before the closing date; incomplete applications will not be considered.",
                ),
                condensed = listOf(
                    "NSFAS applications for 2027 are now open, early submission encouraged.",
                    "Returning students must reapply via the same portal as new applicants.",
                    "Need certified ID, proof of income, and matric results before the deadline.",
                ),
                model = "gemini-2.5-flash",
            ),
            sentiment = SentimentType.POSITIVE,
            tags = listOf("bursary", "nsfas"),
            resources = listOf(
                ResourceLink(
                    "https://www.nsfas.org.za/content/downloads/NSFAS-Application-Form.pdf",
                    "Bursary Application Form (PDF)",
                    ResourceKind.PDF,
                ),
                ResourceLink("https://my.nsfas.org.za/", "Official NSFAS Registration Portal", ResourceKind.PORTAL),
            ),
        ),
        Article(
            id = "eskom-stage-2",
            headline = "Stage 2 Load Shedding Extended Through the Weekend, Eskom Confirms",
            sourceName = "SABC News",
            sourceUrl = "https://www.sabcnews.com/",
            author = null,
            category = "load-shedding",
            imageUrl = null,
            publishedAt = 1_786_188_000_000L,
            readTimeMinutes = 3,
            summary = AiSummary(
                detailed = listOf(
                    "Eskom announced an extension of Stage 2 cuts citing unplanned breakdowns at two coal units. " +
                        "Municipal schedules remain unchanged for now.",
                    "Residents are advised to check their zone schedule via the Eskom app as " +
                        "loadshedding.eskom.co.za reflects the update.",
                ),
                condensed = listOf(
                    "Eskom extends Stage 2 load shedding through the weekend.",
                    "Cause: unplanned breakdowns at two coal-fired units.",
                    "Check your zone schedule via the Eskom app or website.",
                ),
                model = "gemini-2.5-flash",
            ),
            sentiment = SentimentType.NEGATIVE,
            tags = listOf("eskom", "stage2"),
            resources = listOf(
                ResourceLink("https://loadshedding.eskom.co.za/", "Eskom Load Shedding Schedules", ResourceKind.WEB),
            ),
        ),
        Article(
            id = "vodacom-internships",
            headline = "Vodacom Opens 2027 Graduate Internship Programme Across Four Provinces",
            sourceName = "BusinessTech",
            sourceUrl = "https://businesstech.co.za/",
            author = null,
            category = "internships",
            imageUrl = null,
            publishedAt = 1_786_177_200_000L,
            readTimeMinutes = 3,
            summary = AiSummary(
                detailed = listOf(
                    "Vodacom is accepting applications for its 12-month graduate internship, targeting " +
                        "engineering, data and commerce graduates from Gauteng, KZN, the Western and Eastern Cape.",
                    "Successful candidates receive a stipend, a mentor and a structured rotation, with " +
                        "applications closing at the end of the month.",
                ),
                condensed = listOf(
                    "12-month paid internship for engineering, data and commerce graduates.",
                    "Open to applicants in Gauteng, KZN, Western Cape and Eastern Cape.",
                    "Applications close at the end of the month via the Vodacom careers portal.",
                ),
                model = "gemini-2.5-flash",
            ),
            sentiment = SentimentType.NEUTRAL,
            tags = listOf("internship", "graduate"),
            resources = listOf(
                ResourceLink("https://www.vodacom.co.za/vodacom/careers", "Vodacom Careers Portal", ResourceKind.PORTAL),
                ResourceLink("https://www.youtube.com/", "Programme Overview Video", ResourceKind.VIDEO),
            ),
        ),
    )

    val keywords: List<String> = listOf("internship", "load shedding", "bursary")
}
