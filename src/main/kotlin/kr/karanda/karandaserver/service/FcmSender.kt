package kr.karanda.karandaserver.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.WebpushConfig
import kr.karanda.karandaserver.repository.jpa.UserFcmSettingsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * FCM 데이터 메시지 발송 공용 헬퍼.
 *
 * - 한 번에 최대 500개 토큰(multicast 한도)씩 나눠 보낸다.
 * - 응답에서 `UNREGISTERED`(앱 삭제·토큰 만료)인 토큰을 삭제해 발송 대상이 영구히 쌓이지 않게 한다.
 *   `INVALID_ARGUMENT`는 페이로드 오류일 때도 오므로 삭제 기준으로 쓰지 않는다(정상 토큰 일괄 삭제 위험).
 */
@Component
class FcmSender(private val userFcmSettingsRepository: UserFcmSettingsRepository) {
    private val logger: Logger = LoggerFactory.getLogger(FcmSender::class.java)

    /**
     * 데이터 전용 메시지를 보낸다. 표시는 기기가 사용자 언어로 직접 한다.
     *
     * @param ttl 이 시간이 지나도록 전달되지 못한 메시지는 버려진다(늦게 도착한 알림 방지).
     */
    fun sendData(tokens: Collection<String>, data: Map<String, String>, ttl: Duration, logTag: String) {
        val distinct = tokens.distinct()
        if (distinct.isEmpty()) return
        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setTtl(ttl.toMillis())
            .build()
        val webpushConfig = WebpushConfig.builder()
            .putHeader("Urgency", "high")
            .putHeader("TTL", ttl.seconds.toString())
            .build()
        for (chunk in distinct.chunked(MULTICAST_LIMIT)) {
            try {
                val response = FirebaseMessaging.getInstance().sendEachForMulticast(
                    MulticastMessage.builder()
                        .addAllTokens(chunk)
                        .putAllData(data)
                        .setAndroidConfig(androidConfig)
                        .setWebpushConfig(webpushConfig)
                        .build()
                )
                val unregistered = response.responses.withIndex()
                    .filter { (_, result) ->
                        !result.isSuccessful && result.exception?.messagingErrorCode == MessagingErrorCode.UNREGISTERED
                    }
                    .map { chunk[it.index] }
                if (unregistered.isNotEmpty()) {
                    userFcmSettingsRepository.deleteAllByTokenIn(unregistered)
                    logger.info("Removed ${unregistered.size} unregistered FCM tokens ($logTag)")
                }
            } catch (e: Exception) {
                logger.warn("Failed to send FCM multicast ($logTag): ${e.message}")
            }
        }
    }

    companion object {
        private const val MULTICAST_LIMIT = 500
    }
}
