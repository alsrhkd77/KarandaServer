package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.BdoNewsDTO
import kr.karanda.karandaserver.dto.BdoNewsHighlightsDTO
import kr.karanda.karandaserver.dto.BdoNewsNotifyRequest
import kr.karanda.karandaserver.dto.BdoNewsNotifyResponse
import kr.karanda.karandaserver.entity.BdoNews
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsAction
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.repository.jpa.BdoNewsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class BdoNewsService(
    private val bdoNewsRepository: BdoNewsRepository,
    private val notificationService: BdoNewsNotificationService,
) {
    private val logger: Logger = LoggerFactory.getLogger(BdoNewsService::class.java)

    companion object {
        /** 보존 정책이 실패했을 때를 대비한 목록 응답 내부 상한 */
        private const val MAX_LIST_SIZE = 500

        /** NOTICE/UPDATE/LAB 보존 기간 (일) */
        private const val RETENTION_DAYS = 30L

        /** 마감된 EVENT 보존 기간 (일) */
        private const val EVENT_RETENTION_DAYS = 7L
    }

    fun getNews(region: BDORegion, category: BdoNewsCategory?): List<BdoNewsDTO> {
        val pageable = PageRequest.of(
            0, MAX_LIST_SIZE,
            Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
        )
        val news = if (category == null) {
            bdoNewsRepository.findAllByRegion(region.name, pageable)
        } else {
            bdoNewsRepository.findAllByRegionAndCategory(region.name, category.name, pageable)
        }
        return news.map { it.toDTO() }
    }

    fun getHighlights(region: BDORegion): BdoNewsHighlightsDTO {
        val latestMajorUpdate = bdoNewsRepository
            .findFirstByRegionAndCategoryAndIsMajorIsTrueOrderByPublishedAtDescIdDesc(
                region = region.name,
                category = BdoNewsCategory.UPDATE.name,
            )
        val latestLabUpdate = bdoNewsRepository
            .findFirstByCategoryOrderByPublishedAtDescIdDesc(BdoNewsCategory.LAB.name)
        return BdoNewsHighlightsDTO(
            latestMajorUpdate = latestMajorUpdate?.toDTO(),
            latestLabUpdate = latestLabUpdate?.toDTO(),
        )
    }

    /**
     * 크롤러 Webhook 처리.
     *
     * - CREATED: `id` 중복 시 스킵(멱등 — 크롤러의 push 실패 후 재전송을 흡수)
     * - UPDATED: 존재하지 않는 `id`면 무시(방어적), 있으면 deadline/updatedAt 갱신
     *
     * 저장/갱신에 성공한 항목만 실시간 알림(STOMP/FCM)을 발행한다.
     */
    fun processNotify(request: BdoNewsNotifyRequest): BdoNewsNotifyResponse {
        val processed = mutableListOf<Pair<BdoNewsAction, BdoNewsDTO>>()
        for (item in request.items) {
            try {
                when (item.action) {
                    BdoNewsAction.CREATED -> {
                        if (!bdoNewsRepository.existsById(item.id)) {
                            val saved = bdoNewsRepository.save(item.toEntity())
                            processed.add(BdoNewsAction.CREATED to saved.toDTO())
                        }
                    }

                    BdoNewsAction.UPDATED -> {
                        bdoNewsRepository.findByIdOrNull(item.id)?.let { entity: BdoNews ->
                            entity.deadline = item.deadline
                            entity.updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
                            val saved = bdoNewsRepository.save(entity)
                            processed.add(BdoNewsAction.UPDATED to saved.toDTO())
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to process BDO news item (${item.id}, ${item.action}): ${e.message}")
            }
        }
        if (processed.isNotEmpty()) {
            logger.info("BDO news processed: ${processed.size} of ${request.items.size} items")
            notificationService.notifyAll(processed)
        }
        return BdoNewsNotifyResponse(received = request.items.size)
    }

    /**
     * 오래된 뉴스 정리.
     *
     * 별도 스케줄 배치 없이 Webhook 응답 후 비동기로 실행된다
     * (Cloud Run 다중 인스턴스에서 Webhook은 1개 인스턴스만 수신하므로 자연스러운 단일 실행 지점).
     *
     * - NOTICE/UPDATE/LAB: 게시일이 30일 이전
     * - EVENT(마감): deadline이 7일 이전
     * - EVENT(상시, deadline == null): 삭제하지 않음
     */
    @Async
    @Transactional
    fun cleanupOldNews() {
        try {
            val today = LocalDate.now(ZoneId.of("UTC"))
            val deletedNews = bdoNewsRepository.deleteAllByCategoryInAndPublishedAtBefore(
                categories = listOf(
                    BdoNewsCategory.NOTICE.name,
                    BdoNewsCategory.UPDATE.name,
                    BdoNewsCategory.LAB.name,
                ),
                publishedAt = today.minusDays(RETENTION_DAYS),
            )
            val deletedEvents = bdoNewsRepository.deleteAllByCategoryAndDeadlineIsNotNullAndDeadlineBefore(
                category = BdoNewsCategory.EVENT.name,
                deadline = today.minusDays(EVENT_RETENTION_DAYS),
            )
            if (deletedNews > 0 || deletedEvents > 0) {
                logger.info("Deleted old BDO news: $deletedNews news, $deletedEvents ended events")
            }
        } catch (e: Exception) {
            logger.warn("Failed to clean up old BDO news: ${e.message}")
        }
    }
}
