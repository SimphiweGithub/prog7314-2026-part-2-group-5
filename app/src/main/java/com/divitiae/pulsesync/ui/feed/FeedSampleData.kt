package com.divitiae.pulsesync.ui.feed

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
            resourcesExtracted = 3,
            resourcesTotal = 5,
        ),
        ArticleUi(
            id = "eskom-stage-2",
            source = "SABC News",
            category = "Load Shedding",
            timeAgo = "2h ago",
            title = "Stage 2 Load Shedding Extended Through the Weekend, Eskom Confirms",
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
            resourcesExtracted = 2,
            resourcesTotal = 4,
        ),
        ArticleUi(
            id = "vodacom-internships",
            source = "BusinessTech",
            category = "Internships",
            timeAgo = "5h ago",
            title = "Vodacom Opens 2027 Graduate Internship Programme Across Four Provinces",
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
            resourcesExtracted = 4,
            resourcesTotal = 4,
        ),
    )

    fun initialState(): FeedUiState = FeedUiState(
        categories = categories,
        selectedCategory = GENERAL_FEED,
        articles = articles,
    )
}
