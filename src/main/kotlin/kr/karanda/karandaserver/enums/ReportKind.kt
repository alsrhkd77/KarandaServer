package kr.karanda.karandaserver.enums

enum class ReportKind {
    SPOTTED,
    KILLED,
    ACTIVATED,

    /** 서버 전체 점검 시작 (대상·채널 없음) */
    MAINTENANCE_START,

    /** 서버 전체 점검 종료 = 서버 열림 (대상·채널 없음) */
    SERVER_OPEN;

    val isMaintenance: Boolean
        get() = this == MAINTENANCE_START || this == SERVER_OPEN
}
