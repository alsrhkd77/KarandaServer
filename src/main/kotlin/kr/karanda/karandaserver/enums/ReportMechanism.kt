package kr.karanda.karandaserver.enums

/**
 * 제보 대상의 출현/지속 메커니즘. 게임 대상별 규칙이 일관되지 않으므로 수치 몇 개로 일반화하지 않고
 * 메커니즘마다 상태 전이를 따로 둔다.
 */
enum class ReportMechanism(val kinds: Set<ReportKind>) {
    /** 출현 타이밍은 서버 공통, 출현 채널은 랜덤(여러 채널 동시). 재출현 기준 = 회차 출현 시각. */
    SHARED_SCHEDULE(setOf(ReportKind.SPOTTED, ReportKind.KILLED)),

    /** 채널별 독립. 재출현 기준 = 그 채널의 사망 시각. */
    CHANNEL_RESPAWN(setOf(ReportKind.SPOTTED, ReportKind.KILLED)),

    /** 채널별 독립 버프. 다시 활성되면 지속 시간 초기화. */
    BUFF(setOf(ReportKind.ACTIVATED)),
}
