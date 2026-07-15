package kr.karanda.karandaserver.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import com.google.firebase.messaging.WebpushNotification
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.BdoNewsDTO
import kr.karanda.karandaserver.dto.BdoNewsRealtimeMessage
import kr.karanda.karandaserver.dto.BroadcastMessage
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsAction
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.infrastructure.redis.RedisPublisher
import kr.karanda.karandaserver.repository.jpa.UserFcmSettingsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 뉴스 저장/갱신 시 실시간 알림 발행.
 *
 * - STOMP: Cloud Run 다중 인스턴스 동기화를 위해 Redis broadcast 경유
 *   ([RedisPublisher] → 각 인스턴스의 BroadcastMessageListener가 WebSocket 세션에 전달)
 * - FCM 토픽(Android): `news_{region}_{category}` / `news_LAB`
 * - FCM 토큰 개별 발송(Web): 뉴스 알림 설정(newsNotify/newsCategories)이 저장된 토큰 대상.
 *   FCM은 Redis를 경유하지 않으므로 Webhook을 수신한 인스턴스에서만 1회 발송된다.
 */
@Service
class BdoNewsNotificationService(
    private val redisPublisher: RedisPublisher,
    private val userFcmSettingsRepository: UserFcmSettingsRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(BdoNewsNotificationService::class.java)

    /**
     * FCM 발송은 블로킹 네트워크 호출이므로 Webhook 요청 스레드를 잡지 않도록 비동기로 실행한다.
     * (별도 빈에서 호출되어야 `@Async` 프록시가 동작한다 — [BdoNewsService]가 호출.)
     * 데이터는 이미 저장된 뒤이므로, 발송 실패는 로그만 남기고 크롤러 응답에는 영향을 주지 않는다.
     */
    @Async
    fun notifyAll(items: List<Pair<BdoNewsAction, BdoNewsDTO>>) {
        if (items.isEmpty()) return
        publishStomp(items)
        publishFcm(items)
    }

    private fun publishStomp(items: List<Pair<BdoNewsAction, BdoNewsDTO>>) {
        try {
            val messages = items.map { (action, news) ->
                BroadcastMessage(
                    destinations = mutableListOf(stompDestination(news)),
                    message = Json.encodeToString(BdoNewsRealtimeMessage.from(action, news)),
                )
            }
            redisPublisher.broadcast(messages)
        } catch (e: Exception) {
            logger.warn("Failed to publish BDO news to STOMP: ${e.message}")
        }
    }

    private fun publishFcm(items: List<Pair<BdoNewsAction, BdoNewsDTO>>) {
        // Web 개별 발송 대상은 한 번만 조회해 항목별로 필터링
        val newsNotifySettings = try {
            userFcmSettingsRepository.findAllByNewsNotifyIsTrue()
        } catch (e: Exception) {
            logger.warn("Failed to load news FCM settings: ${e.message}")
            emptyList()
        }

        for ((action, news) in items) {
            val data = buildFcmData(action, news)
            // 클라이언트에 flutter_local_notifications가 없으므로, 백그라운드에서도
            // 시스템 알림이 자동 표시되도록 data와 함께 notification payload를 포함한다.
            // 제목/본문은 뉴스 리전의 언어로 구성 (뉴스 본문 언어와 일치).
            val title = notificationTitle(action, news)
            val body = notificationBody(action, news)
            val notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()

            // Android — 토픽 발행 (구독은 클라이언트가 직접 관리)
            try {
                FirebaseMessaging.getInstance().send(
                    Message.builder()
                        .setTopic(fcmTopic(news))
                        .putAllData(data)
                        .setNotification(notification)
                        .setAndroidConfig(
                            AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build()
                        )
                        .build()
                )
            } catch (e: Exception) {
                logger.warn("Failed to send BDO news FCM topic message (${news.id}): ${e.message}")
            }

            // Web — 토큰 개별 발송 (Web SDK는 토픽 구독 미지원).
            // 알림 클릭 시 이동할 링크는 수신자 리전에 따라 달라질 수 있어(LAB 언어 치환) 링크별로 묶어 발송.
            newsNotifySettings.filter { settings ->
                val categoryMatched = settings.newsCategories.split(",").contains(news.category.name)
                // LAB 뉴스는 리전 무관, 리전 뉴스는 설정된 리전과 일치해야 함
                val regionMatched = news.category == BdoNewsCategory.LAB || settings.region == news.region.name
                categoryMatched && regionMatched
            }.groupBy { settings ->
                resolveWebLink(news, settings.region)
            }.forEach { (link, settings) ->
                sendFcmToTokens(
                    tokens = settings.map { it.token },
                    data = data,
                    title = title,
                    body = body,
                    link = link,
                    newsId = news.id,
                )
            }
        }
    }

    private fun sendFcmToTokens(
        tokens: List<String>,
        data: Map<String, String>,
        title: String,
        body: String,
        link: String,
        newsId: String,
    ) {
        if (tokens.isEmpty()) return
        // 브라우저·설치형 PWA 알림에 앱 아이콘/배지가 표시되도록 WebpushNotification을 명시한다
        // (기존 PartyFinderService 발송 패턴과 동일). 백그라운드 표시는 firebase-messaging-sw.js의
        // FCM 기본 핸들러가 담당하고, 클릭 시 이동은 FcmOptions.link로 처리된다.
        val webpushConfig = WebpushConfig.builder()
            .setFcmOptions(WebpushFcmOptions.withLink(link))
            .setNotification(
                WebpushNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setIcon("https://www.karanda.kr/icons/android-chrome-512x512.png")
                    .setBadge("https://www.karanda.kr/icons/android-chrome-192x192.png")
                    .build()
            )
            .build()
        // MulticastMessage는 한 번에 최대 500개 토큰까지 허용
        for (chunk in tokens.chunked(500)) {
            try {
                FirebaseMessaging.getInstance().sendEachForMulticast(
                    MulticastMessage.builder()
                        .addAllTokens(chunk)
                        .putAllData(data)
                        .setWebpushConfig(webpushConfig)
                        .build()
                )
            } catch (e: Exception) {
                logger.warn("Failed to send BDO news FCM multicast (${newsId}): ${e.message}")
            }
        }
    }

    /** LAB 뉴스 URL은 ko-KR 기준으로 저장되므로 KR 외 리전 수신자는 en-US로 치환 */
    private fun resolveWebLink(news: BdoNewsDTO, settingsRegion: String): String {
        return if (news.region == BDORegion.LAB && settingsRegion != BDORegion.KR.name) {
            news.url.replaceFirst("/ko-KR/", "/en-US/")
        } else {
            news.url
        }
    }

    private fun notificationTitle(action: BdoNewsAction, news: BdoNewsDTO): String {
        return when (news.region) {
            BDORegion.KR, BDORegion.LAB -> when (action) {
                BdoNewsAction.CREATED -> "[${categoryNameKo(news.category)}] 신규 소식"
                BdoNewsAction.UPDATED -> "[이벤트 연장]"
            }

            BDORegion.JP -> when (action) {
                BdoNewsAction.CREATED -> "[${categoryNameJa(news.category)}] 新着ニュース"
                BdoNewsAction.UPDATED -> "[イベント延長]"
            }

            BDORegion.NA, BDORegion.EU -> when (action) {
                BdoNewsAction.CREATED -> "[${categoryNameEn(news.category)}] News"
                BdoNewsAction.UPDATED -> "[Event Extended]"
            }
        }
    }

    private fun notificationBody(action: BdoNewsAction, news: BdoNewsDTO): String {
        return when (action) {
            BdoNewsAction.CREATED -> news.title
            BdoNewsAction.UPDATED -> "${news.title} (~${news.deadline ?: "-"})"
        }
    }

    private fun categoryNameKo(category: BdoNewsCategory): String = when (category) {
        BdoNewsCategory.NOTICE -> "공지사항"
        BdoNewsCategory.UPDATE -> "업데이트"
        BdoNewsCategory.EVENT -> "이벤트"
        BdoNewsCategory.LAB -> "연구소"
    }

    private fun categoryNameJa(category: BdoNewsCategory): String = when (category) {
        BdoNewsCategory.NOTICE -> "お知らせ"
        BdoNewsCategory.UPDATE -> "アップデート"
        BdoNewsCategory.EVENT -> "イベント"
        BdoNewsCategory.LAB -> "研究所"
    }

    private fun categoryNameEn(category: BdoNewsCategory): String = when (category) {
        BdoNewsCategory.NOTICE -> "Notice"
        BdoNewsCategory.UPDATE -> "Update"
        BdoNewsCategory.EVENT -> "Event"
        BdoNewsCategory.LAB -> "Global Lab"
    }

    private fun buildFcmData(action: BdoNewsAction, news: BdoNewsDTO): Map<String, String> {
        return mapOf(
            "type" to "BDO_NEWS",
            "action" to action.name,
            "id" to news.id,
            "region" to news.region.name,
            "category" to news.category.name,
            "title" to news.title,
            "url" to news.url,
            "thumbnail" to (news.thumbnail ?: ""),
            "publishedAt" to news.publishedAt.toString(),
            "isMajor" to news.isMajor.toString(),
            "deadline" to (news.deadline?.toString() ?: ""),
        )
    }

    private fun stompDestination(news: BdoNewsDTO): String {
        return if (news.region == BDORegion.LAB) {
            "/live-data/bdo-news/LAB"
        } else {
            "/live-data/bdo-news/${news.region.name}/${news.category.name}"
        }
    }

    private fun fcmTopic(news: BdoNewsDTO): String {
        return if (news.region == BDORegion.LAB) {
            "news_LAB"
        } else {
            "news_${news.region.name}_${news.category.name}"
        }
    }
}
