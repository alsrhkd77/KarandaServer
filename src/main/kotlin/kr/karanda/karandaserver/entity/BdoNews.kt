package kr.karanda.karandaserver.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.karanda.karandaserver.dto.BdoNewsDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsCategory
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity(name = "bdo_news")
@Table(
    name = "bdo_news",
    indexes = [
        Index(name = "idx_bdo_news_list", columnList = "region, category, published_at DESC"),
        Index(name = "idx_bdo_news_major", columnList = "region, is_major, published_at DESC"),
        Index(name = "idx_bdo_news_event_cleanup", columnList = "category, deadline"),
        Index(name = "idx_bdo_news_cleanup", columnList = "category, published_at"),
    ]
)
class BdoNews(
    /** `{REGION}_{CATEGORY}_{원본ID}`. LAB은 `LAB_{_boardNo}` */
    @Id
    @Column(length = 60)
    var id: String,
    @Column(length = 10, nullable = false)
    var region: String,
    @Column(length = 10, nullable = false)
    var category: String,
    @Column(length = 500, nullable = false)
    var title: String,
    @Column(length = 1000, nullable = false)
    var url: String,
    @Column(length = 1000)
    var thumbnail: String? = null,
    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDate,
    /** 최초 크롤링 시각 (UTC) */
    @Column(name = "crawled_at", nullable = false)
    var crawledAt: ZonedDateTime,
    @Column(name = "created_at", nullable = false)
    var createdAt: ZonedDateTime,
    /** 마지막 갱신 시각 (이벤트 연장 등) */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime,
    /** 주요 업데이트 여부. UPDATE/LAB 카테고리에서만 의미 있음 */
    @Column(name = "is_major", nullable = false)
    var isMajor: Boolean = false,
    /** 이벤트 마감일. EVENT 카테고리 전용, null이면 상시 이벤트 */
    var deadline: LocalDate? = null,
) {
    fun toDTO(): BdoNewsDTO {
        return BdoNewsDTO(
            id = id,
            region = BDORegion.valueOf(region),
            category = BdoNewsCategory.valueOf(category),
            title = title,
            url = url,
            thumbnail = thumbnail,
            publishedAt = publishedAt,
            isMajor = isMajor,
            deadline = deadline,
        )
    }
}
