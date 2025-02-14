package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.data.DiscordExchangeCodeResponse
import kr.karanda.karandaserver.data.DiscordProperties
import kr.karanda.karandaserver.data.DiscordUserDataResponse
import kr.karanda.karandaserver.repository.DefaultDataRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.client.RestClient

@Service
class DiscordService(private val defaultDataRepository: DefaultDataRepository) {

    private val properties: DiscordProperties
        get() = defaultDataRepository.getDiscordProperties()
    val client = RestClient.create(properties.api)

    fun exchangeCode(code: String, redirectUrl: String): String {
        val data = mapOf(
            "client_id" to listOf(properties.clientId),
            "client_secret" to listOf(properties.clientSecret),
            "grant_type" to listOf("authorization_code"),
            "code" to listOf(code),
            //"scope" to listOf("identify email"),
            "scope" to listOf("identify"),
            "redirect_uri" to listOf(redirectUrl),
        )
        val response = client.post()
            .uri("/oauth2/token")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            //.body(BodyInserters.fromFormData(CollectionUtils.toMultiValueMap(data)))
            .body(CollectionUtils.toMultiValueMap(data))
            .retrieve()
            .body(DiscordExchangeCodeResponse::class.java)
        return response!!.access_token
    }

    fun getUserDataByToken(token: String): DiscordUserDataResponse {
        val response = client.get()
            .uri("/users/@me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .body(DiscordUserDataResponse::class.java)
        return response!!
    }

    fun getUserDataById(discordId: String): DiscordUserDataResponse {
        val response = client.get()
            .uri("/users/$discordId")
            .header(HttpHeaders.AUTHORIZATION, "Bot ${properties.token}")
            .retrieve()
            .body(DiscordUserDataResponse::class.java) ?: throw Exception()
        return response
    }
}