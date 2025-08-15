package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion
import java.time.ZonedDateTime

data class ReportDTO(
    val id: Long?,
    val target: String,
    val region: BDORegion,
    val channel: String?,
    val active: Boolean,
    val eventAt: ZonedDateTime,
    val reportedAt: ZonedDateTime,
    val reporter: UserDTO?,
    val isCanceled: Boolean,
    val cancelReason: String?
)
