package kr.karanda.karandaserver

import io.jsonwebtoken.Jwts
import kr.karanda.karandaserver.data.TokenProperties
import kr.karanda.karandaserver.repository.UserRepository
import kr.karanda.karandaserver.service.FireStoreService
import kr.karanda.karandaserver.service.UserService
import kr.karanda.karandaserver.util.TokenFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import javax.crypto.spec.SecretKeySpec


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [KarandaServerApplication::class])
class DiscordAuthControllerTests {


    @Autowired
    private lateinit var tokenFactory: TokenFactory

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var fireStoreService: FireStoreService

    @Autowired
    private lateinit var wac: WebApplicationContext

    private lateinit var tokenProperties: TokenProperties
    private lateinit var discordId: String
    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        discordId = userRepository.findAll().first().discordId
        println("discordId: $discordId")
        tokenProperties = fireStoreService.getTokenProperties()
        client = MockMvcWebTestClient.bindToApplicationContext(wac)
            //.baseUrl("http://localhost:8000")
            //.defaultHeader("Qualification", tokenFactory.createQualificationToken())
            .build()
    }

    @Test
    fun `Assert successful authentication`() {
        val user = userService.getByDiscordId(discordId)
        val token = tokenFactory.createTokens(user!!.userUUID, user.username)
        client.get().uri("/auth/discord/authorization")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token.accessToken}")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Assert authentication with expired token`() {
        val user = userService.getByDiscordId(discordId)
        val token = createExpiredAccessToken(user!!.userUUID, user.username)
        client.get().uri("/auth/discord/authorization")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    fun createExpiredAccessToken(userUUID: String, username: String): String {
        val random = Random()
        val claims = mutableMapOf("username" to username)
        val now = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(random.nextLong(1, 12345))

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .claims(claims)
            .signWith(SecretKeySpec(tokenProperties.secretKey.toByteArray(), tokenProperties.algorithm))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }
}