package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.ReportHubChannelStatus
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import org.springframework.data.jpa.repository.JpaRepository

interface ReportHubChannelStatusRepository : JpaRepository<ReportHubChannelStatus, Long> {
    fun findByRegionAndTargetAndChannel(region: BDORegion, target: ReportTarget, channel: String): ReportHubChannelStatus?
    fun findAllByRegion(region: BDORegion): List<ReportHubChannelStatus>
}
