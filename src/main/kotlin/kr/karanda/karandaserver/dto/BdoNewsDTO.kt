package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsAction
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.util.LocalDateSerializer
import java.time.LocalDate

/**
 * 뉴스 목록/하이라이트 응답 항목.
 */
@Serializable
data class BdoNewsDTO(
    val id: String,
    val region: BDORegion,
    val category: BdoNewsCategory,
    val title: String,
    val url: String,
    val thumbnail: String?,
    @Serializable(with = LocalDateSerializer::class)
    val publishedAt: LocalDate,
    val isMajor: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val deadline: LocalDate?,
)

/**
 * 홈 화면용 주요 뉴스 응답 (`GET /bdo-news/highlights`).
 */
@Serializable
data class BdoNewsHighlightsDTO(
    val latestMajorUpdate: BdoNewsDTO?,
    val latestLabUpdate: BdoNewsDTO?,
)

/**
 * STOMP 실시간 발행 payload (뉴스 항목 + action).
 */
@Serializable
data class BdoNewsRealtimeMessage(
    val action: BdoNewsAction,
    val id: String,
    val region: BDORegion,
    val category: BdoNewsCategory,
    val title: String,
    val url: String,
    val thumbnail: String?,
    @Serializable(with = LocalDateSerializer::class)
    val publishedAt: LocalDate,
    val isMajor: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val deadline: LocalDate?,
) {
    companion object {
        fun from(action: BdoNewsAction, news: BdoNewsDTO): BdoNewsRealtimeMessage {
            return BdoNewsRealtimeMessage(
                action = action,
                id = news.id,
                region = news.region,
                category = news.category,
                title = news.title,
                url = news.url,
                thumbnail = news.thumbnail,
                publishedAt = news.publishedAt,
                isMajor = news.isMajor,
                deadline = news.deadline,
            )
        }
    }
}
