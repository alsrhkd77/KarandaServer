package kr.karanda.karandaserver.data

import java.time.ZoneId
import java.time.ZonedDateTime

data class ParsedFamilyProfile(
    val region: String,
    var mainClass: String = "unknown",
    val familyName: String,
    val createdOn: String,
    var contributionPoints: String,
    var lifeSkillLevel: FamilyLifeSkillLevel? = null,
    val verificationOn: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
)
