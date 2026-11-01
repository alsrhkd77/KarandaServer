package kr.karanda.karandaserver.service.reporthub

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId

/**
 * 지역에서 쓰는 대상 정책. 수치를 지정하지 않으면 [ReportTarget]의 기본값(KR 기준)을 쓴다.
 */
data class ReportTargetPolicy(
    val target: ReportTarget,
    val respawnMin: Duration? = target.respawnMin,
    val respawnMax: Duration? = target.respawnMax,
    val respawnDelay: Duration? = target.respawnDelay,
    val lifetime: Duration? = target.lifetime,
) {
    val mechanism: ReportMechanism
        get() = target.mechanism
}

/**
 * Reporter Hub 지역 설정.
 *
 * 지역을 여는 일은 이 설정을 추가하는 것만으로 끝나야 한다(서비스·규칙 코드는 지역을 모른다).
 * 다른 지역의 도메인 사실(채널 목록, 대상 존재 여부, 수치, 정기점검 요일)은 추측해서 채우지 않는다.
 *
 * @property channels 그 지역의 채널 코드. 코드는 지역 무관 이름 코드(`{area}_{nn}`)라 이름이 같은 채널은 지역이 달라도 같은 코드다.
 * @property regularMaintenanceDay 정기점검 요일. null이면 미상 → 모든 날에 2인 확정 규칙 적용.
 */
data class ReportHubRegionConfig(
    val region: BDORegion,
    val enabled: Boolean,
    val channels: List<String>,
    val targets: List<ReportTargetPolicy>,
    val regularMaintenanceDay: DayOfWeek?,
) {
    private val policies = targets.associateBy { it.target }
    private val channelSet = channels.toSet()

    val timezone: ZoneId
        get() = region.timezone

    fun policyOf(target: ReportTarget): ReportTargetPolicy? = policies[target]

    fun hasChannel(channel: String): Boolean = channel in channelSet
}

object ReportHubRegions {
    /**
     * KR 채널 (2026-11-01 사용자 확인). 1~3채널: 울루키타·세렌디아·카마실비아·발렌시아·발레노스·에페리아·하이델·칼페온·메디아·에다니아,
     * 1채널뿐: 올비아·오딜리타·아르샤·아르샤: 무명, 파트리지오 1~2, 시즌 1~4. 순서가 곧 화면 표시 순서다.
     * 채널 코드는 `{area}_{nn}`, 채널이 하나뿐인 area는 번호 없이 `{area}`다(클라이언트가 코드만 보고 이름을 정한다).
     */
    private val krChannels: List<String> = buildList {
        listOf(
            "ulukita", "serendia", "kamasylvia", "valencia", "balenos",
            "epheria", "heidel", "calpheon", "mediah", "edania",
        ).forEach { area -> (1..3).forEach { add("%s_%02d".format(area, it)) } }
        addAll(listOf("olvia", "odyllita", "arsha", "arsha_nameless"))
        (1..2).forEach { add("patrigio_%02d".format(it)) }
        (1..4).forEach { add("season_%02d".format(it)) }
    }

    private val configs: Map<BDORegion, ReportHubRegionConfig> = listOf(
        ReportHubRegionConfig(
            region = BDORegion.KR,
            enabled = true,
            channels = krChannels,
            targets = ReportTarget.entries.map { ReportTargetPolicy(it) },
            regularMaintenanceDay = DayOfWeek.WEDNESDAY,
        ),
    ).associateBy { it.region }

    /** 활성 지역의 설정. 비활성이거나 설정이 없으면 null. */
    fun enabled(region: BDORegion): ReportHubRegionConfig? = configs[region]?.takeIf { it.enabled }

    val enabledRegions: List<BDORegion>
        get() = configs.values.filter { it.enabled }.map { it.region }
}
