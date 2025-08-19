package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

data class ReportStatusDTO(
    val id: Long?,
    val target: ReportTarget,
    val region: BDORegion,
    val channel: String?,
    val active: Boolean,
    val eventAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val sourceReport: ReportDTO?
)
