package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class Applicant(
    val code: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val joinAt: ZonedDateTime,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val cancelledAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val rejectedAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val acceptedAt: ZonedDateTime?,
    val user: UserDTO,
    val postId: Long
)
