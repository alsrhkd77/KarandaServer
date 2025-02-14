package kr.karanda.karandaserver.repository

import jakarta.annotation.PostConstruct
import kr.karanda.karandaserver.data.SafeBrowsingApiProperties
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestClient

@Repository
class SafeUrlRepository(private val defaultDataRepository: DefaultDataRepository) {
    private lateinit var client: RestClient

    val properties: SafeBrowsingApiProperties
        get() = defaultDataRepository.getSafeBrowsingApiProperties()

    @PostConstruct
    fun initialize() {
        client = RestClient.builder()
            .baseUrl("https://safebrowsing.googleapis.com/v4/threatMatches:find")
            .defaultUriVariables(mapOf("key" to properties.apiKey))
            .build()
    }

    fun check(urls: List<String>): List<String> {
        val payload = mapOf(
            "client" to mapOf(
                "clientId" to properties.clientId,
                "clientVersion" to "0.0.1"
            ),
            "threatInfo" to mapOf(
                "threatTypes" to listOf(
                    "THREAT_TYPE_UNSPECIFIED",
                    "MALWARE",
                    "SOCIAL_ENGINEERING",
                    "UNWANTED_SOFTWARE",
                    //"POTENTIALLY_HARMFUL_APPLICATION"
                ),
                "platformTypes" to listOf("ANY_PLATFORM"),
                "threatEntryTypes" to listOf("URL"),
                "threatEntries" to urls.map { mapOf("url" to it) }
            )
        )
        val response = client
            .post()
            .contentType(APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(UrlCheckResponse::class.java)
        return response?.urls() ?: emptyList()
    }
}

private data class UrlCheckResponse(val matches: List<Match>) {
    fun treats(): List<Treat> {
        return matches.map { it.treat }
    }

    fun urls(): List<String> {
        return matches.map { it.treat.url }
    }
}

private data class Match(
    val threatType: String,
    val platformType: String,
    val threatEntryType: String,
    val treat: Treat,
    val threatEntryMetadata: List<Map<String, String>>,
    val cacheDuration: String
)

private data class Treat(val url: String)