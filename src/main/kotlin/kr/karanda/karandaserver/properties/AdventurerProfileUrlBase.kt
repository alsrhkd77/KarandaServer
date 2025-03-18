package kr.karanda.karandaserver.properties

import kr.karanda.karandaserver.enums.BDORegion
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "karanda.adventurer-profile-url-base")
data class AdventurerProfileUrlBase(
    var kr: String,
    var na: String,
    var eu: String,
    var jp: String,
) {
    fun getUrl(region: BDORegion): String {
        return when (region) {
            BDORegion.KR -> kr
            BDORegion.NA -> na
            BDORegion.EU -> eu
        }
    }
}
