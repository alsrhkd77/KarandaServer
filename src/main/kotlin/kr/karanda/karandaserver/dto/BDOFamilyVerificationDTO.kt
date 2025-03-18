package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

data class BDOFamilyVerificationDTO(
    val lifeSkillIsLocked: Boolean,
    val contributionPointIsLocked: Boolean,
    val createdAt: ZonedDateTime,
)
