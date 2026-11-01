package kr.karanda.karandaserver

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.karanda.karandaserver.entity.User
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.TokenUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reporter Hub 실시간 경로 통합 검증: 실제 서버(랜덤 포트) + STOMP 클라이언트.
 *
 * 로그인 사용자의 HTTP 제보 → Redis → STOMP 상태 델타, 그리고 20초 뒤 Windows용 알림 이벤트,
 * 5초 안에 취소한 제보는 알림이 나가지 않는 것까지 확인한다. 알림 지연 때문에 30초가량 걸린다.
 *
 * 실행: `SPRING_PROFILES_ACTIVE=develop ./gradlew integrationTest --tests "*ReportHubRealtimeIntegrationTests"`
 * 인메모리 DB를 써서 로컬 개발 DB(H2 파일)와 실행 중인 dev 서버에 영향을 주지 않는다(Redis 채널은 공유).
 */
@Tag("integration")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.datasource.url=jdbc:h2:mem:report-hub-realtime;DB_CLOSE_DELAY=-1"],
)
class ReportHubRealtimeIntegrationTests {
    @LocalServerPort private var port: Int = 0
    @Autowired private lateinit var tokenUtils: TokenUtils
    @Autowired private lateinit var userRepository: UserRepository

    private val http = HttpClient.newHttpClient()
    private val sessions = mutableListOf<StompSession>()

    @AfterEach
    fun tearDown() = sessions.forEach { runCatching { it.disconnect() } }

    private fun newUser(): User {
        val suffix = UUID.randomUUID().toString().take(8)
        return userRepository.save(
            User(
                userUUID = UUID.randomUUID().toString(),
                discordId = "rh-rt-$suffix",
                discordDiscriminator = "0",
                userName = "rh-rt-$suffix",
                avatarHash = null,
            )
        )
    }

    private fun subscribe(destination: String): LinkedBlockingQueue<String> {
        val client = WebSocketStompClient(StandardWebSocketClient()).apply {
            messageConverter = StringMessageConverter()
        }
        val connectHeaders = StompHeaders().apply { add("Qualification", tokenUtils.createQualificationToken()) }
        val session = client.connectAsync(
            "ws://localhost:$port/live-channel", WebSocketHttpHeaders(), connectHeaders, object : StompSessionHandlerAdapter() {},
        ).get(10, TimeUnit.SECONDS)
        sessions += session
        val queue = LinkedBlockingQueue<String>()
        session.subscribe(destination, object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type = String::class.java
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                queue.add(payload as String)
            }
        })
        Thread.sleep(500) // SUBSCRIBE 처리 대기
        return queue
    }

    private fun post(user: User, path: String, body: String): HttpResponse<String> {
        val token = tokenUtils.createTokens(user.userUUID, user.userName).accessToken
        return http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port$path"))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
    }

    private fun patch(user: User, path: String): HttpResponse<String> {
        val token = tokenUtils.createTokens(user.userUUID, user.userName).accessToken
        return http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port$path"))
                .header("Authorization", "Bearer $token")
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
    }

    private fun reportBody(channel: String) =
        """{"region":"KR","target":"MARETTA","channel":"$channel","kind":"KILLED","eventAt":"${Instant.now().truncatedTo(ChronoUnit.SECONDS)}"}"""

    @Test
    fun `report reaches subscribers as delta, alert follows after delay, canceled report sends no alert`() {
        val status = subscribe("/live-data/report-hub/KR/status")
        val alerts = subscribe("/live-data/report-hub/KR/alert")
        val reporter = newUser()

        // 1) 유지되는 제보: 델타가 즉시 온다
        val kept = post(reporter, "/report-hub/report", reportBody("mediah_01"))
        assertEquals(201, kept.statusCode(), kept.body())
        val delta = status.poll(10, TimeUnit.SECONDS)
        assertNotNull(delta, "상태 델타가 STOMP로 전달되어야 한다")
        val channel = Json.parseToJsonElement(delta).jsonObject["channels"].toString()
        assertTrue(channel.contains("mediah_01") && channel.contains("KILLED"), delta)

        // 2) 곧바로 취소할 제보 (다른 채널)
        val canceled = post(reporter, "/report-hub/report", reportBody("mediah_03"))
        assertEquals(201, canceled.statusCode(), canceled.body())
        val canceledId = Json.parseToJsonElement(canceled.body()).jsonObject["report"]!!.jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(200, patch(reporter, "/report-hub/cancel?reportId=$canceledId").statusCode())

        // 3) 20초 지연 후 유지된 제보의 알림만 온다
        val received = mutableListOf<String>()
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            alerts.poll(1, TimeUnit.SECONDS)?.let { received += it }
            if (received.isNotEmpty() && System.currentTimeMillis() > deadline - 5_000) break
        }
        val alert = received.map { Json.parseToJsonElement(it).jsonObject }
        assertTrue(alert.any { it["channel"]?.jsonPrimitive?.content == "mediah_01" && it["reason"]?.jsonPrimitive?.content == "FOLLOWED" },
            "유지된 제보의 알림이 와야 한다: $received")
        assertNull(alert.firstOrNull { it["channel"]?.jsonPrimitive?.content == "mediah_03" },
            "5초 안에 취소한 제보는 알림이 나가면 안 된다: $received")
        assertEquals("REPORT_HUB", alert.first()["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `anonymous can read but cannot write`() {
        val read = http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/report-hub/status?region=KR")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertEquals(200, read.statusCode())
        assertTrue(read.body().contains("\"sessions\""), "빈 목록도 필드로 내려가야 한다: ${read.body()}")
        val write = http.send(
            HttpRequest.newBuilder(URI("http://localhost:$port/report-hub/report"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reportBody("mediah_02")))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertTrue(write.statusCode() == 401 || write.statusCode() == 403, "비로그인 제보는 거부: ${write.statusCode()}")
    }
}
