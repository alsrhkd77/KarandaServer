package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubSession
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import org.springframework.data.jpa.repository.JpaRepository

interface ReportHubSessionRepository : JpaRepository<ReportHubSession, Long> {
    fun findFirstByRegionAndTargetAndCanceledFalseOrderByAnchorAtDescIdDesc(
        region: BDORegion,
        target: ReportTarget,
    ): ReportHubSession?
}
