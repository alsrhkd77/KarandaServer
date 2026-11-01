package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubReport
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface ReportHubReportRepository : JpaRepository<ReportHubReport, Long> {

    /** 이 제보자가 현재 채널 상태에 이미 기여했는지 (동조 중복 방지) */
    fun existsByRegionAndTargetAndChannelAndReporterIdAndKindAndCanceledFalseAndEventAtGreaterThanEqualAndIdNot(
        region: BDORegion,
        target: ReportTarget,
        channel: String,
        reporterId: Long,
        kind: ReportKind,
        eventAt: ZonedDateTime,
        id: Long,
    ): Boolean

    /** SHARED: 이 제보자가 같은 회차·채널 상태에 이미 기여했는지 */
    fun existsBySessionIdAndChannelAndReporterIdAndKindAndCanceledFalseAndIdNot(
        sessionId: Long,
        channel: String,
        reporterId: Long,
        kind: ReportKind,
        id: Long,
    ): Boolean

    /** SHARED: 이 제보자가 같은 회차의 출현을 이미 제보했는지 */
    fun existsBySessionIdAndReporterIdAndKindAndCanceledFalseAndIdNot(
        sessionId: Long,
        reporterId: Long,
        kind: ReportKind,
        id: Long,
    ): Boolean

    /** 취소 후 재계산용 (MARETTA·BUFF) */
    fun findAllByRegionAndTargetAndChannelAndCanceledFalseAndReportedAtGreaterThanEqualOrderByReportedAtAscIdAsc(
        region: BDORegion,
        target: ReportTarget,
        channel: String,
        reportedAt: ZonedDateTime,
    ): List<ReportHubReport>

    /** 취소 후 재계산용 (SHARED 채널) */
    fun findAllBySessionIdAndChannelAndCanceledFalseOrderByReportedAtAscIdAsc(
        sessionId: Long,
        channel: String,
    ): List<ReportHubReport>

    /** 취소 후 재계산용 (SHARED 회차) */
    fun findAllBySessionIdAndKindAndCanceledFalseOrderByEventAtAscIdAsc(
        sessionId: Long,
        kind: ReportKind,
    ): List<ReportHubReport>

    fun findAllByMaintenanceIdAndCanceledFalse(maintenanceId: Long): List<ReportHubReport>

    fun findTop20ByRegionAndTargetAndChannelOrderByReportedAtDesc(
        region: BDORegion,
        target: ReportTarget,
        channel: String,
    ): List<ReportHubReport>

    fun findTop20ByRegionAndReporterIdOrderByReportedAtDesc(region: BDORegion, reporterId: Long): List<ReportHubReport>

    /** 지역 리더보드: 기간 내 유효 제보 수 상위 제보자 */
    @Query(
        "select r.reporterId as reporterId, count(r) as reportCount from ReportHubReport r " +
                "where r.region = :region and r.reportedAt >= :since and r.canceled = false " +
                "group by r.reporterId order by count(r) desc"
    )
    fun countRecentByReporter(
        @Param("region") region: BDORegion,
        @Param("since") since: ZonedDateTime,
        pageable: Pageable,
    ): List<ReporterReportCount>

    interface ReporterReportCount {
        val reporterId: Long
        val reportCount: Long
    }
}
