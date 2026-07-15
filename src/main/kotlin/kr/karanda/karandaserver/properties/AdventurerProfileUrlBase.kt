package kr.karanda.karandaserver.properties

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.InvalidArgumentException
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
            BDORegion.JP -> jp
            // LAB(연구소)은 뉴스 전용 리전으로 모험가 프로필이 없음
            BDORegion.LAB -> throw InvalidArgumentException()
        }
    }
}
