package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class BDOFamilyDTO(
    var code: String,
    var region: BDORegion,
    var familyName: String,
    var mainClass: String,
    var maxGearScore: Int?,
    var verified: Boolean,
    @Serializable(with = ZonedDateTimeSerializer::class)
    var lastUpdated: ZonedDateTime?,
)
