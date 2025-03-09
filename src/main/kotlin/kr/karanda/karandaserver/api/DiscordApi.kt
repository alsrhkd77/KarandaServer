package kr.karanda.karandaserver.api

import kr.karanda.karandaserver.dto.DiscordProperties
import kr.karanda.karandaserver.dto.DiscordUserData
import kr.karanda.karandaserver.exception.ExternalApiException
import kr.karanda.karandaserver.repository.DefaultDataRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import org.springframework.web.client.RestClient

/**
 * **Discord Api** 사용 클래스.
 *
 * **Discord OAuth2**에 사용됨.
 *
 * @property [defaultDataRepository]
 * @property [logger]
 * @property [client]
 * @property [properties]
 *
 * @see [DefaultDataRepository]
 */
@Component
class DiscordApi(private val defaultDataRepository: DefaultDataRepository) {
    val logger: Logger = LoggerFactory.getLogger(DiscordApi::class.java)
    private val client = RestClient.create(properties.api)

    private val properties: DiscordProperties
        get() = defaultDataRepository.getDiscordProperties()

    /**
     * [code]를 **Discord Access Token**으로 교환.
     * 응답을 [DiscordExchangeCodeResponse]로 mapping 한 뒤 [DiscordExchangeCodeResponse.access_token]을 반환
     *
     * @param [code] 사용자가 **Discord** 로그인을 통해 얻을 수 있음.
     * @param [redirectURL] **Discord 개발자 센터**에 등록된 URL.
     * @return [DiscordExchangeCodeResponse.access_token] (String)
     * @throws [ExternalApiException] Api에서 200이 아닌 상태를 반환했거나 응답이 비어있을 경우 throw.
     */
    fun exchangeCode(code: String, redirectURL: String): String {
        val payload = mapOf(
            "client_id" to listOf(properties.clientId),
            "client_secret" to listOf(properties.clientSecret),
            "grant_type" to listOf("authorization_code"),
            "code" to listOf(code),
            //"scope" to listOf("identify email"),
            "scope" to listOf("identify"),
            "redirect_uri" to listOf(redirectURL),
        )
        try {
            val response = client.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(CollectionUtils.toMultiValueMap(payload))
                .retrieve()
                .body(DiscordExchangeCodeResponse::class.java) ?: throw ExternalApiException()
            return response.access_token
        } catch (e: ExternalApiException) {
            logger.warn("Failed to exchange code with the Discord API. Response body is null", e)
            throw ExternalApiException()
        } catch (e: Exception) {
            logger.warn("Failed to exchange code with the Discord API.", e)
            throw ExternalApiException()
        }
    }

    /**
     * **Discord Access Token**을 사용해 [DiscordUserData]를 가져옴.
     *
     * @param [token] **Discord Access Token**
     * @throws [ExternalApiException] Api에서 200이 아닌 상태를 반환했거나 응답이 비어있을 경우 throw.
     */
    fun getUserDataByToken(token: String): DiscordUserData {
        try {
            return client.get()
                .uri("/users/@me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .body(DiscordUserData::class.java) ?: throw ExternalApiException()
        } catch (e: ExternalApiException) {
            logger.warn("Failed to exchange code with the Discord API. Response body is null", e)
            throw ExternalApiException()
        } catch (e: Exception) {
            logger.warn("Failed to exchange code with the Discord API.", e)
            throw ExternalApiException()
        }
    }

    /**
     * 유저의 [discordId]를 사용해 [DiscordUserData]를 가져옴.
     *
     * @throws [ExternalApiException] Api에서 200이 아닌 상태를 반환했거나 응답이 비어있을 경우 throw.
     */
    fun getUserDataById(discordId: String): DiscordUserData {
        try {
            return client.get()
                .uri("/users/$discordId")
                .header(HttpHeaders.AUTHORIZATION, "Bot ${properties.token}")
                .retrieve()
                .body(DiscordUserData::class.java) ?: throw ExternalApiException()
        } catch (e: ExternalApiException) {
            logger.warn("Failed to exchange code with the Discord API. Response body is null", e)
            throw ExternalApiException()
        } catch (e: Exception) {
            logger.warn("Failed to exchange code with the Discord API.", e)
            throw ExternalApiException()
        }
    }
}

/**
 * 토큰 교환을 위한 요청 후  반환된 응답 mapping Class.
 * 응답 형식에 맞춰 **snake case**를 사용.
 *
 * @see [kr.karanda.karandaserver.api.DiscordApi.exchangeCode]
 */
private data class DiscordExchangeCodeResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String,
)

