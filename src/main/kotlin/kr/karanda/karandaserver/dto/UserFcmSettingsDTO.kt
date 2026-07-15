package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.BdoNewsCategory

@Serializable
data class UserFcmSettingsDTO(
    var token: String,
    var region: BDORegion,
    var partyFinder: Boolean = false,
    var fieldBoss: Boolean = false,
    /** 뉴스 알림 설정 (Web 전용). 기존 save API는 이 필드들을 갱신하지 않는다 — PATCH /fcm/settings/news 사용 */
    var newsNotify: Boolean = false,
    var newsCategories: List<BdoNewsCategory> = emptyList(),
)
