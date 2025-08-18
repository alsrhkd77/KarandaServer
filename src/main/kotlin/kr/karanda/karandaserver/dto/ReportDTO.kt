package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.ZonedDateTime

data class ReportDTO(
    val id: Long?,
    val target: ReportTarget,
    val region: BDORegion,
    val channel: String?,
    val active: Boolean,
    val eventAt: ZonedDateTime,
    val reportedAt: ZonedDateTime,
    val reporter: UserDTO?,
    val isCanceled: Boolean,
    val cancelReason: String?
)
