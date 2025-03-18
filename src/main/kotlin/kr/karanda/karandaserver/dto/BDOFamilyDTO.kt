package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion

@Serializable
data class BDOFamilyDTO(
    var code: String,
    var region: BDORegion,
    var familyName: String,
    var mainClass: String,
    var verified: Boolean,
)
