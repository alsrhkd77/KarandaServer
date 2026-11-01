package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.ReportHubRelationDTO
import kr.karanda.karandaserver.entity.ReportHubRelation
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubRelationType
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.repository.jpa.ReportHubRelationRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReporterRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.service.reporthub.ReportHubRegions
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reporter Hub 팔로우/차단. 팔로우는 신뢰 필터다 — 팔로우한 제보자의 제보만 알림으로 받는다.
 * 팔로워 수는 제보자 통계 행에서 원자적으로 증감한다.
 */
@Service
class ReportHubRelationService(
    private val userRepository: UserRepository,
    private val relationRepository: ReportHubRelationRepository,
    private val reporterRepository: ReportHubReporterRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)

    fun getRelations(userUUID: String, region: BDORegion): List<ReportHubRelationDTO> {
        val userId = userId(userUUID)
        return transaction.execute {
            val relations = relationRepository.findAllByUserIdAndRegion(userId, region)
            val reporters = reporterRepository.findAllByRegionAndUserIdIn(region, relations.map { it.reporterId })
                .associateBy { it.userId }
            relations.map { ReportHubRelationDTO(it.reporterId, it.type, reporters[it.reporterId]?.toSummaryDTO()) }
        }!!
    }

    /** 팔로우 또는 차단. 같은 쌍은 둘 중 하나만 가지므로 팔로우 → 차단 전환도 이 메서드로 한다. */
    fun setRelation(userUUID: String, region: BDORegion, reporterId: Long, type: ReportHubRelationType): ReportHubRelationDTO {
        val userId = userId(userUUID)
        if (ReportHubRegions.enabled(region) == null || reporterId == userId) throw InvalidArgumentException()
        return try {
            applyRelation(userId, region, reporterId, type)
        } catch (_: DataIntegrityViolationException) {
            // 같은 요청이 동시에 들어와 다른 쪽이 먼저 만들었다 — 다시 반영한다
            applyRelation(userId, region, reporterId, type)
        }
    }

    private fun applyRelation(userId: Long, region: BDORegion, reporterId: Long, type: ReportHubRelationType): ReportHubRelationDTO =
        transaction.execute {
            // 이 지역에서 제보한 적이 있는 사용자만 대상으로 한다
            val reporter = reporterRepository.findByUserIdAndRegion(reporterId, region) ?: throw InvalidArgumentException()
            val existing = relationRepository.findByUserIdAndReporterIdAndRegion(userId, reporterId, region)
            val previousType = existing?.type
            var followerDelta = 0L
            if (previousType != type) {
                if (type == ReportHubRelationType.FOLLOW &&
                    relationRepository.countByUserIdAndRegionAndType(userId, region, ReportHubRelationType.FOLLOW) >= MAX_FOLLOWS
                ) {
                    throw InvalidArgumentException()
                }
                if (existing == null) {
                    relationRepository.saveAndFlush(
                        ReportHubRelation(
                            userId = userId,
                            reporterId = reporterId,
                            region = region,
                            type = type,
                            createdAt = ZonedDateTime.now(ZoneId.of("UTC")),
                        )
                    )
                } else {
                    existing.type = type
                    relationRepository.save(existing)
                }
                followerDelta = when {
                    type == ReportHubRelationType.FOLLOW -> 1L
                    previousType == ReportHubRelationType.FOLLOW -> -1L
                    else -> 0L
                }
                if (followerDelta != 0L) reporterRepository.addFollowerCount(reporterId, region, followerDelta)
            }
            // 관리 중인 엔티티를 바꾸면 커밋 때 그 값으로 덮어써 원자적 증감이 깨지므로 응답 DTO에서만 반영한다
            val summary = reporter.toSummaryDTO().let { it.copy(followerCount = it.followerCount + followerDelta) }
            ReportHubRelationDTO(reporterId, type, summary)
        }!!

    fun removeRelation(userUUID: String, region: BDORegion, reporterId: Long) {
        val userId = userId(userUUID)
        transaction.executeWithoutResult {
            val existing = relationRepository.findByUserIdAndReporterIdAndRegion(userId, reporterId, region)
                ?: return@executeWithoutResult
            relationRepository.delete(existing)
            if (existing.type == ReportHubRelationType.FOLLOW) {
                reporterRepository.addFollowerCount(reporterId, region, -1)
            }
        }
    }

    /**
     * 회원 탈퇴 시 정리: 이 사용자가 맺은 관계(팔로우했던 제보자의 팔로워 수 차감 포함),
     * 이 사용자를 대상으로 한 관계, 제보자 통계를 지운다. 제보 원장은 id만 남는다.
     */
    fun onUserDeleted(userId: Long) {
        transaction.executeWithoutResult {
            val relations = relationRepository.findAllByUserIdOrReporterId(userId, userId)
            relations.filter { it.userId == userId && it.type == ReportHubRelationType.FOLLOW }
                .forEach { reporterRepository.addFollowerCount(it.reporterId, it.region, -1) }
            relationRepository.deleteAll(relations)
            reporterRepository.deleteAllByUserId(userId)
        }
    }

    private fun userId(userUUID: String): Long =
        userRepository.findByUserUUID(userUUID)?.id ?: throw UnknownUserException()

    companion object {
        private const val MAX_FOLLOWS = 200L
    }
}
