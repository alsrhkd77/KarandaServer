package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BdoNews
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BdoNewsRepository : JpaRepository<BdoNews, String> {
    fun findAllByRegion(region: String, pageable: Pageable): List<BdoNews>

    fun findAllByRegionAndCategory(region: String, category: String, pageable: Pageable): List<BdoNews>

    fun findFirstByRegionAndCategoryAndIsMajorIsTrueOrderByPublishedAtDescIdDesc(
        region: String,
        category: String,
    ): BdoNews?

    fun findFirstByCategoryOrderByPublishedAtDescIdDesc(category: String): BdoNews?

    fun deleteAllByCategoryInAndPublishedAtBefore(categories: List<String>, publishedAt: LocalDate): Long

    fun deleteAllByCategoryAndDeadlineIsNotNullAndDeadlineBefore(category: String, deadline: LocalDate): Long
}
