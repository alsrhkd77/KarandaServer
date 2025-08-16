package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion
import java.time.ZonedDateTime

data class ReportStatusDTO(
    val id: Long?,
    val target: String,
    val region: BDORegion,
    val channel: String?,
    val active: Boolean,
    val eventTime: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val sourceReport: ReportDTO
)
