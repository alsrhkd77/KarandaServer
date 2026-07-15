package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.enums.BdoNewsCategory

/**
 * 뉴스 알림 설정 저장 요청 (`PATCH /fcm/settings/news`, Web 전용).
 */
@Serializable
data class BdoNewsNotificationSettingsRequest(
    val token: String,
    val newsNotify: Boolean,
    val newsCategories: List<BdoNewsCategory> = emptyList(),
)
