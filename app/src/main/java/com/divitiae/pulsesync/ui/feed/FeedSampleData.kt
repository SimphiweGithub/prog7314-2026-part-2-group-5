package com.divitiae.pulsesync.ui.feed

/**
 * Code Attribution No 16
 * This method was taken from "Compose Preview sample data providers"
 * https://developer.android.com/develop/ui/compose/tooling/previews
 * Android Developers
 */

/**
 * Placeholder content mirroring the Figma Feed Dashboard frames. Used by
 * previews and by the prototype until the Retrofit-backed ViewModel lands.
 */
object FeedSampleData {
    const val GENERAL_FEED = "General Feed"

    val categories = listOf(GENERAL_FEED, "Bursaries", "Load Shedding", "Internships")

    val articles = listOf(
        ArticleUi(
            id = "nsfas-2027",
            source = "News24",
            category = "Bursaries",
            timeAgo = "2h ago",
            title = "NSFAS Opens 2027 Bursary Applications for First-Year Students",
            sourceUrl = "https://www.news24.com/",
            body = listOf(
                "NSFAS has officially opened applications for the 2027 academic year, with early submission " +
                    "strongly encouraged. The scheme confirmed that returning students must reapply through " +
                    "the same online portal used for new applicants.",
                "Applicants will need a certified copy of their ID, proof of household income, and their " +
                    "latest matric or academic results uploaded before the closing date. NSFAS has warned that " +
                    "incomplete applications will not be considered, and encouraged students to double-check " +
                    "banking details to avoid disbursement delays.",
                "Students who are unsure of their eligibility can use the means-test calculator on the NSFAS " +
                    "portal before submitting supporting documents.",
            ),
            detailedSummary = listOf(
                "NSFAS has officially opened applications for the 2027 academic year, with early " +
                    "submission strongly encouraged. The scheme confirmed that returning students must reapply.",
                "Applicants will need a certified ID copy, proof of household income, and matric results " +
                    "uploaded via the online portal before the closing date.",
            ),
            condensedSummary = listOf(
                "NSFAS applications for 2027 are now open, early submission encouraged.",
                "Returning students must reapply via the same portal as new applicants.",
                "Need certified ID, proof of income, and matric results before the deadline.",
            ),
            sentiment = Sentiment.POSITIVE,
            keywords = listOf("bursary", "nsfas"),
            resources = listOf(
                ResourceLinkUi(
                    title = "Bursary Application Form (PDF)",
                    url = "https://www.nsfas.org.za/content/downloads/NSFAS-Application-Form.pdf",
                    type = ResourceType.PDF,
                ),
                ResourceLinkUi(
                    title = "Official NSFAS Registration Portal",
                    url = "https://my.nsfas.org.za/",
                    type = ResourceType.PORTAL,
                ),
            ),
            offlineSlotsUsed = 3,
            offlineSlotsTotal = 5,
            note = NoteUi(
                title = "NSFAS application checklist",
                text = "Deadline is in 3 weeks — need certified ID copy + latest results. Ask study group if " +
                    "anyone has done means test already.",
                tags = listOf("Bursaries 2026", "To-do"),
                syncState = NoteSyncState.SYNCED,
            ),
        ),
        ArticleUi(
            id = "eskom-stage-2",
            source = "SABC News",
            category = "Load Shedding",
            timeAgo = "2h ago",
            title = "Stage 2 Load Shedding Extended Through the Weekend, Eskom Confirms",
            sourceUrl = "https://www.sabcnews.com/",
            body = listOf(
                "Eskom announced an extension of Stage 2 cuts citing unplanned breakdowns at two coal units. " +
                    "Municipal schedules remain unchanged for now, and the utility expects generation to " +
                    "recover once the units return to service early next week.",
                "Residents in affected areas are advised to check their zone schedule via the Eskom app, as " +
                    "loadshedding.eskom.co.za reflects the update. Municipal customers should confirm with " +
                    "their local supplier, as some municipalities publish their own block schedules.",
            ),
            detailedSummary = listOf(
                "Eskom announced an extension of Stage 2 cuts citing unplanned breakdowns at two coal " +
                    "units. Municipal schedules remain unchanged for now.",
                "Residents in affected areas are advised to check their zone schedule via the Eskom app, " +
                    "as loadshedding.eskom.co.za reflects the update.",
            ),
            condensedSummary = listOf(
                "Eskom extends Stage 2 load shedding through the weekend.",
                "Cause: unplanned breakdowns at two coal-fired units.",
                "Check your zone schedule via the Eskom app or website.",
            ),
            sentiment = Sentiment.NEGATIVE,
            keywords = listOf("eskom", "stage2"),
            resources = listOf(
                ResourceLinkUi(
                    title = "Eskom Load Shedding Schedules",
                    url = "https://loadshedding.eskom.co.za/",
                    type = ResourceType.WEB,
                ),
                ResourceLinkUi(
                    title = "Eskom Media Statement (PDF)",
                    url = "https://www.eskom.co.za/wp-content/uploads/media-statement.pdf",
                    type = ResourceType.PDF,
                ),
            ),
            offlineSlotsUsed = 3,
            offlineSlotsTotal = 5,
        ),
        ArticleUi(
            id = "vodacom-internships",
            source = "BusinessTech",
            category = "Internships",
            timeAgo = "5h ago",
            title = "Vodacom Opens 2027 Graduate Internship Programme Across Four Provinces",
            sourceUrl = "https://businesstech.co.za/",
            body = listOf(
                "Vodacom is accepting applications for its 12-month graduate internship, targeting engineering, " +
                    "data and commerce graduates from Gauteng, KZN, the Western and Eastern Cape.",
                "Successful candidates receive a stipend, a mentor and a structured rotation across business " +
                    "units, with applications closing at the end of the month. Shortlisted applicants will be " +
                    "invited to a virtual assessment day in the first week of the following month.",
            ),
            detailedSummary = listOf(
                "Vodacom is accepting applications for its 12-month graduate internship, targeting " +
                    "engineering, data and commerce graduates from Gauteng, KZN, the Western and Eastern Cape.",
                "Successful candidates receive a stipend, a mentor and a structured rotation across " +
                    "business units, with applications closing at the end of the month.",
            ),
            condensedSummary = listOf(
                "12-month paid internship for engineering, data and commerce graduates.",
                "Open to applicants in Gauteng, KZN, Western Cape and Eastern Cape.",
                "Applications close at the end of the month via the Vodacom careers portal.",
            ),
            sentiment = Sentiment.NEUTRAL,
            keywords = listOf("internship", "graduate"),
            resources = listOf(
                ResourceLinkUi(
                    title = "Vodacom Careers Portal",
                    url = "https://www.vodacom.co.za/vodacom/careers",
                    type = ResourceType.PORTAL,
                ),
                ResourceLinkUi(
                    title = "Programme Overview Video",
                    url = "https://www.youtube.com/",
                    type = ResourceType.VIDEO,
                ),
            ),
            offlineSlotsUsed = 3,
            offlineSlotsTotal = 5,
        ),
    )

    fun articleById(id: String): ArticleUi? = articles.firstOrNull { it.id == id }

    fun initialState(): FeedUiState = FeedUiState(
        categories = categories,
        selectedCategory = GENERAL_FEED,
        articles = articles,
    )
}
