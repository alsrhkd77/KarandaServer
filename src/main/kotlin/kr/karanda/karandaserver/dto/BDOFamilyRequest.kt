package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion

data class BDOFamilyRequest(
    val code: String,
    val region: BDORegion,
    val familyName: String,
)
