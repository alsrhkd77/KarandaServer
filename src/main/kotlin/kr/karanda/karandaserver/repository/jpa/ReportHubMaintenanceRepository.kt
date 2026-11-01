package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubMaintenance
import kr.karanda.karandaserver.enums.BDORegion
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface ReportHubMaintenanceRepository : JpaRepository<ReportHubMaintenance, Long> {
    fun findAllByRegionAndCanceledFalseAndUpdatedAtAfterOrderByIdDesc(
        region: BDORegion,
        updatedAt: ZonedDateTime,
    ): List<ReportHubMaintenance>

    fun findTop2ByRegionAndCanceledFalseOrderByIdDesc(region: BDORegion): List<ReportHubMaintenance>
}
