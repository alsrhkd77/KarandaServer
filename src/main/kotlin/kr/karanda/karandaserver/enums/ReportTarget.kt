package kr.karanda.karandaserver.enums

import java.time.Duration

/**
 * Reporter Hub 제보 대상.
 *
 * 메커니즘과 KR 기준 기본 수치만 가진다. 어느 지역에서 활성인지, 지역별로 수치가 다른지는
 * [kr.karanda.karandaserver.service.reporthub.ReportHubRegions]의 지역 설정이 정한다.
 *
 * @property respawnMin SHARED_SCHEDULE: 이전 출현 시각으로부터 최소 재출현 시간
 * @property respawnMax SHARED_SCHEDULE: 이전 출현 시각으로부터 최대 재출현 시간
 * @property respawnDelay CHANNEL_RESPAWN: 사망 후 같은 채널 재출현까지의 시간
 * @property lifetime SHARED_SCHEDULE: 출현 후 자연 소멸까지의 시간, BUFF: 지속 시간
 */
enum class ReportTarget(
    val mechanism: ReportMechanism,
    val respawnMin: Duration? = null,
    val respawnMax: Duration? = null,
    val respawnDelay: Duration? = null,
    val lifetime: Duration? = null,
) {
    BHEG(ReportMechanism.SHARED_SCHEDULE, Duration.ofHours(12), Duration.ofHours(19), lifetime = Duration.ofMinutes(30)),
    MUDSTER(ReportMechanism.SHARED_SCHEDULE, Duration.ofHours(12), Duration.ofHours(19), lifetime = Duration.ofMinutes(30)),
    WHALE(ReportMechanism.SHARED_SCHEDULE, Duration.ofHours(14), Duration.ofHours(25), lifetime = Duration.ofMinutes(30)),
    MARETTA(ReportMechanism.CHANNEL_RESPAWN, respawnDelay = Duration.ofHours(1)),
    SUN(ReportMechanism.BUFF, lifetime = Duration.ofHours(1)),
    MOON(ReportMechanism.BUFF, lifetime = Duration.ofHours(1)),
    EARTH(ReportMechanism.BUFF, lifetime = Duration.ofHours(1)),
    GOLDEN_BELL(ReportMechanism.BUFF, lifetime = Duration.ofHours(1)),
}
