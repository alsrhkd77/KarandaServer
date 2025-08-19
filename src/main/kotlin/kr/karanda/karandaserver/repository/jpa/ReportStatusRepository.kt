package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportStatus
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReportStatusRepository: JpaRepository<ReportStatus, Long> {
    fun findByTargetAndRegionAndChannel(target: ReportTarget, region: BDORegion, channel: String?): ReportStatus?
    fun findBySourceReportId(sourceReportId: Long): ReportStatus?

    @EntityGraph(attributePaths = ["sourceReport", "sourceReport.reporter"])
    fun findAllByRegion(region: BDORegion): List<ReportStatus>

    @EntityGraph(attributePaths = ["sourceReport", "sourceReport.reporter"])
    fun findAllByTargetAndRegion(target: ReportTarget, region: BDORegion): List<ReportStatus>
}