package kr.karanda.karandaserver.enums

/** 알림을 받는 이유. Windows 클라이언트는 이 값으로 표시 여부를 판단한다. */
enum class ReportHubAlertReason {
    /** 팔로우한 제보자가 이벤트를 만들거나 동조함 */
    FOLLOWED,

    /** 서로 다른 제보자 수가 검증 기준에 도달함 (팔로우가 0명인 사용자 대상) */
    VERIFIED,

    /** 서버 전체 점검 종료 확정 */
    MAINTENANCE,
}
