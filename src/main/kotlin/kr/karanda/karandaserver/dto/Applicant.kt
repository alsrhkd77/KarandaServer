package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class Applicant(
    val code: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val appliedAt: ZonedDateTime,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val canceledAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val rejectedAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val approvedAt: ZonedDateTime?,
    val reason: String?,
    val user: User,
    val postId: Long
)
