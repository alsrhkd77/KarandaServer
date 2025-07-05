package kr.karanda.karandaserver.api

import jakarta.annotation.PostConstruct
import kr.karanda.karandaserver.dto.properties.SafeBrowsingApiProperties
import kr.karanda.karandaserver.repository.DefaultDataProvider
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * **Safe Browsing Api** 사용 클래스
 * 유해 URL 확인을 위해 사용
 *
 * V5 문서가 완성되면 마이그레이션 해야할 수 있음.
 */
@Component
class SafeUrlApi(private val defaultDataProvider: DefaultDataProvider) {
    private lateinit var client: RestClient

    val properties: SafeBrowsingApiProperties
        get() = defaultDataProvider.getSafeBrowsingApiProperties()

    @PostConstruct
    fun initialize() {
        client = RestClient.builder()
            .baseUrl("https://safebrowsing.googleapis.com/v4/threatMatches:find")
            .defaultUriVariables(mapOf("key" to properties.apiKey))
            .build()
    }

    /**
     * URL들을 확인하고 안전하지 않은 URL들을 반환.
     *
     * @param [urls] 확인할 URL 목록.
     * @return 안전하지 않은 URL 목록.
     */
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
