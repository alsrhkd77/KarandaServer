package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable

/**
 * Reporter Hub 알림 설정 저장 요청 (`PATCH /fcm/settings/report-hub`, Web·Android).
 *
 * @property targets 알림을 받을 대상 코드(ReportTarget 이름)와 서버 열림 알림용 `MAINTENANCE`
 */
@Serializable
data class ReportHubNotificationSettingsRequest(
    val token: String,
    val notify: Boolean,
    val targets: List<String> = emptyList(),
)
