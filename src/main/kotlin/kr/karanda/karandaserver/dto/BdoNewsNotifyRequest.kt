package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.entity.BdoNews
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsAction
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.util.LocalDateSerializer
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 크롤러 Webhook 요청 (`POST /bdo-news/notify`).
 */
@Serializable
data class BdoNewsNotifyRequest(
    val items: List<BdoNewsNotifyItem>,
)

@Serializable
data class BdoNewsNotifyItem(
    val action: BdoNewsAction,
    val id: String,
    val region: BDORegion,
    val category: BdoNewsCategory,
    val title: String,
    val url: String,
    /** 크롤러는 썸네일이 없으면 빈 문자열을 보냄 */
    val thumbnail: String = "",
    @Serializable(with = LocalDateSerializer::class)
    val publishedAt: LocalDate,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val crawledAt: ZonedDateTime,
    val isMajor: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val deadline: LocalDate? = null,
) {
    fun toEntity(): BdoNews {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        return BdoNews(
            id = id,
            region = region.name,
            category = category.name,
            title = title,
            url = url,
            thumbnail = thumbnail.ifEmpty { null },
            publishedAt = publishedAt,
            crawledAt = crawledAt,
            createdAt = now,
            updatedAt = now,
            isMajor = isMajor,
            deadline = deadline,
        )
    }
}

/**
 * 크롤러 Webhook 응답.
 */
@Serializable
data class BdoNewsNotifyResponse(
    val received: Int,
)
