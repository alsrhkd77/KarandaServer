package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

data class BDOFamily(
    val familyName: String,
    val mainClass: String,
    val region: String,
    val code: String,
    var verified: Boolean,
    var lifeSkillIsPrivate: Boolean,
    var startVerification: ZonedDateTime? = null,
    var firstVerification: ZonedDateTime? = null,
    var secondVerification: ZonedDateTime? = null,
    var lastUpdated: ZonedDateTime? = null,
)
