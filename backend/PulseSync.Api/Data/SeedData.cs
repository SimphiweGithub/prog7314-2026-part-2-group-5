using PulseSync.Api.Models;

namespace PulseSync.Api.Data;

public static class SeedData
{
    public static readonly List<CategoryDto> Categories = new()
    {
        new("bursaries", "Bursaries", "Iimali zezifundo", "Beurse", true),
        new("load-shedding", "Load Shedding", "Ukucinywa kukagesi", "Beurtkrag", true),
        new("internships", "Internships", "Izifundo zomsebenzi", "Internskappe", true),
        new("technology", "Technology", "Ubuchwepheshe", "Tegnologie", false)
    };

    public static readonly List<ArticleDto> Articles = new()
    {
        new(
            ArticleId: "nsfas-2027",
            Headline: "NSFAS Opens 2027 Bursary Applications for First-Year Students",
            SourceName: "News24",
            SourceUrl: "https://www.news24.com/",
            Author: null,
            Category: "bursaries",
            ImageUrl: null,
            PublishedAt: 1786195200000L,
            ReadTimeMinutes: 4,
            AiSummary: new AiSummaryDto(
                Detailed: new List<string>
                {
                    "NSFAS has officially opened applications for the 2027 academic year, with early submission strongly encouraged. Returning students must reapply through the same portal.",
                    "Applicants need a certified ID copy, proof of household income, and matric results uploaded before the closing date; incomplete applications will not be considered."
                },
                Condensed: new List<string>
                {
                    "NSFAS applications for 2027 are now open, early submission encouraged.",
                    "Returning students must reapply via the same portal as new applicants.",
                    "Need certified ID, proof of income, and matric results before the deadline."
                },
                Model: "gemini-2.5-flash",
                GeneratedAt: 1786195250000L
            ),
            Sentiment: new SentimentDto(0.8, "positive"),
            DerivedTags: new List<string> { "bursary", "nsfas" },
            ExtractedLinks: new List<ExtractedLinkDto>
            {
                new("https://www.nsfas.org.za/content/downloads/NSFAS-Application-Form.pdf", "Bursary Application Form (PDF)", "PDF"),
                new("https://my.nsfas.org.za/", "Official NSFAS Registration Portal", "PORTAL")
            }
        ),
        new(
            ArticleId: "eskom-stage-2",
            Headline: "Stage 2 Load Shedding Extended Through the Weekend, Eskom Confirms",
            SourceName: "SABC News",
            SourceUrl: "https://www.sabcnews.com/",
            Author: null,
            Category: "load-shedding",
            ImageUrl: null,
            PublishedAt: 1786188000000L,
            ReadTimeMinutes: 3,
            AiSummary: new AiSummaryDto(
                Detailed: new List<string>
                {
                    "Eskom announced an extension of Stage 2 cuts citing unplanned breakdowns at two coal units. Municipal schedules remain unchanged for now.",
                    "Residents are advised to check their zone schedule via the Eskom app as loadshedding.eskom.co.za reflects the update."
                },
                Condensed: new List<string>
                {
                    "Eskom extends Stage 2 load shedding through the weekend.",
                    "Cause: unplanned breakdowns at two coal-fired units.",
                    "Check your zone schedule via the Eskom app or website."
                },
                Model: "gemini-2.5-flash",
                GeneratedAt: 1786188050000L
            ),
            Sentiment: new SentimentDto(-0.6, "negative"),
            DerivedTags: new List<string> { "eskom", "stage2" },
            ExtractedLinks: new List<ExtractedLinkDto>
            {
                new("https://loadshedding.eskom.co.za/", "Eskom Load Shedding Schedules", "WEB")
            }
        ),
        new(
            ArticleId: "vodacom-internships",
            Headline: "Vodacom Opens 2027 Graduate Internship Programme Across Four Provinces",
            SourceName: "BusinessTech",
            SourceUrl: "https://businesstech.co.za/",
            Author: null,
            Category: "internships",
            ImageUrl: null,
            PublishedAt: 1786177200000L,
            ReadTimeMinutes: 3,
            AiSummary: new AiSummaryDto(
                Detailed: new List<string>
                {
                    "Vodacom has launched its 2027 graduate recruitment drive targeting IT, engineering, and data science graduates across South Africa.",
                    "Successful candidates will rotate through telecommunications and cloud divisions during the 18-month paid development programme."
                },
                Condensed: new List<string>
                {
                    "Vodacom launches 2027 graduate internship drive for IT and engineering.",
                    "18-month paid programme rotating through cloud and telecom divisions.",
                    "Applications close at the end of the month."
                },
                Model: "gemini-2.5-flash",
                GeneratedAt: 1786177250000L
            ),
            Sentiment: new SentimentDto(0.7, "positive"),
            DerivedTags: new List<string> { "internship", "vodacom" },
            ExtractedLinks: new List<ExtractedLinkDto>
            {
                new("https://www.vodacom.com/careers/graduates", "Apply: Vodacom Graduate Portal", "PORTAL")
            }
        ),
        new(
            ArticleId: "deepseek-local-rag",
            Headline: "Running Open-Source AI Locally: Offline Edge LLMs for Constrained Connectivity",
            SourceName: "TechCentral",
            SourceUrl: "https://techcentral.co.za/",
            Author: null,
            Category: "technology",
            ImageUrl: null,
            PublishedAt: 1786162800000L,
            ReadTimeMinutes: 5,
            AiSummary: new AiSummaryDto(
                Detailed: new List<string>
                {
                    "Recent breakthroughs in quantized open models enable sub-7B parameter LLMs to run efficiently on mobile neural processing units.",
                    "This architecture allows offline text summarization and indexing during power and connectivity outages without cloud latency."
                },
                Condensed: new List<string>
                {
                    "Quantized small LLMs now run on edge mobile hardware.",
                    "Enables local AI summarization and note indexing during outages.",
                    "Reduces reliance on constant cloud connectivity."
                },
                Model: "gemini-2.5-flash",
                GeneratedAt: 1786162850000L
            ),
            Sentiment: new SentimentDto(0.5, "positive"),
            DerivedTags: new List<string> { "ai", "technology", "edge" },
            ExtractedLinks: new List<ExtractedLinkDto>
            {
                new("https://techcentral.co.za/edge-ai-south-africa", "Full Research Paper (PDF)", "PDF")
            }
        )
    };

    public static readonly Dictionary<string, FullTextDto> FullTexts = new()
    {
        ["nsfas-2027"] = new(
            ArticleId: "nsfas-2027",
            SanitizedText: "The National Student Financial Aid Scheme (NSFAS) has officially opened student applications for the 2027 academic year. Minister of Higher Education urged all prospective learners to submit early. Documentation required includes a certified copy of South African ID card or green barcode identity document, household income verification, and recent school results. Applicants from households earning less than R350,000 per annum qualify for full tuition, accommodation, and study allowances.",
            WordCount: 88,
            SizeBytes: 620,
            RetrievedAt: 1786195200000L
        ),
        ["eskom-stage-2"] = new(
            ArticleId: "eskom-stage-2",
            SanitizedText: "Eskom confirmed today that Stage 2 load shedding will remain in force over the upcoming weekend to replenish emergency generation reserves. Unplanned unit trips at Tutuka and Medupi power stations removed 1,850 MW from the national grid. The power utility requests citizens to continue switching off non-essential appliances.",
            WordCount: 52,
            SizeBytes: 390,
            RetrievedAt: 1786188000000L
        )
    };
}
