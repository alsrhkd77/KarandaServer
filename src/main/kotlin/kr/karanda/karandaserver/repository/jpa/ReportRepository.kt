package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.Report
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface ReportRepository : JpaRepository<Report, Long> {
    fun findByIdAndReporterUserUUIDAndIsCanceledFalse(
        id: Long,
        userUUID: String
    ): Report?

    fun findTop1ByTargetAndRegionAndChannelAndIsCanceledFalseOrderByEventAtDesc(
        target: ReportTarget,
        region: BDORegion,
        channel: String?
    ): Report?

    fun findTop1ByTargetAndRegionAndIsCanceledFalseOrderByEventAtDesc(target: ReportTarget, region: BDORegion): Report?

    fun findTop1ByTargetAndRegionAndActiveAndEventAtBetweenAndIsCanceledFalseOrderByEventAtAsc(
        target: ReportTarget,
        region: BDORegion,
        active: Boolean,
        eventAtStart: ZonedDateTime,
        eventAtEnd: ZonedDateTime
    ): Report?

    fun findAllByTargetAndRegionAndEventAtAfterOrderByEventAtAsc(
        target: ReportTarget,
        region: BDORegion,
        eventAt: ZonedDateTime
    ): List<Report>
}