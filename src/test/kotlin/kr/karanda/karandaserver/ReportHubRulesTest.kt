package kr.karanda.karandaserver

import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportMechanism
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.service.reporthub.ReportHubRegionConfig
import kr.karanda.karandaserver.service.reporthub.ReportHubRegions
import kr.karanda.karandaserver.service.reporthub.ReportHubRules
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.ChannelReplayItem
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.ChannelState
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.Decision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.MaintenanceDecision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.MaintenanceState
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.SessionDecision
import kr.karanda.karandaserver.service.reporthub.ReportHubRules.SessionState
import kr.karanda.karandaserver.service.reporthub.ReportTargetPolicy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [ReportHubRules] 단위 검증 — Spring 컨텍스트 불필요.
 */
@Tag("unit")
class ReportHubRulesTest {
    /** 2026-11-04(수) 12:00 UTC = KST 21:00 수요일 */
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 11, 4, 12, 0, 0, 0, ZoneOffset.UTC)
    private val kr = ReportHubRegions.enabled(BDORegion.KR)!!

    /** 지역 격리 검증용 가짜 두 번째 지역(채널 2개, 마레타·태양만, 정기점검 요일 미상) */
    private val testRegion = ReportHubRegionConfig(
        region = BDORegion.NA,
        enabled = true,
        channels = listOf("grana_01", "balenos_01"),
        targets = listOf(ReportTargetPolicy(ReportTarget.MARETTA), ReportTargetPolicy(ReportTarget.SUN)),
        regularMaintenanceDay = null,
    )

    private fun minutes(value: Long): ZonedDateTime = now.plusMinutes(value)

    @Nested
    inner class Validate {
        @Test
        fun `boss report keeps client eventAt`() {
            val result = ReportHubRules.validate(kr, ReportTarget.MARETTA, "balenos_01", ReportKind.KILLED, minutes(-5), null, now)
            assertEquals(minutes(-5), result.eventAt)
        }

        @Test
        fun `buff start is derived from remaining minutes`() {
            val result = ReportHubRules.validate(kr, ReportTarget.SUN, "balenos_01", ReportKind.ACTIVATED, null, 40, now)
            assertEquals(minutes(-20), result.eventAt)
        }

        @Test
        fun `buff remaining minutes out of range is rejected`() {
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.SUN, "balenos_01", ReportKind.ACTIVATED, null, 61, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.SUN, "balenos_01", ReportKind.ACTIVATED, null, 0, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.SUN, "balenos_01", ReportKind.ACTIVATED, null, null, now)
            }
        }

        @Test
        fun `eventAt outside allowed window is rejected`() {
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.BHEG, "balenos_01", ReportKind.SPOTTED, minutes(-16), null, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.BHEG, "balenos_01", ReportKind.SPOTTED, minutes(2), null, now)
            }
        }

        @Test
        fun `kind must match mechanism`() {
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.SUN, "balenos_01", ReportKind.KILLED, minutes(0), 30, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.MARETTA, "balenos_01", ReportKind.ACTIVATED, minutes(0), null, now)
            }
        }

        @Test
        fun `unknown channel is rejected`() {
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.MARETTA, "balenos_99", ReportKind.KILLED, minutes(0), null, now)
            }
        }

        @Test
        fun `removed targets no longer exist`() {
            // 라우라우(월드보스 전환)와 골든 케이브는 제보 대상이 아니다. 요청의 enum 역직렬화 단계에서 400이 된다.
            assertFalse(ReportTarget.entries.any { it.name == "RAWR_RAWR" || it.name == "GOLDEN_CAVE" })
        }

        @Test
        fun `maintenance report has no target or channel`() {
            val result = ReportHubRules.validate(kr, null, null, ReportKind.SERVER_OPEN, minutes(0), null, now)
            assertNull(result.target)
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.MARETTA, null, ReportKind.SERVER_OPEN, minutes(0), null, now)
            }
        }

        @Test
        fun `region isolation - disabled region and foreign codes are rejected`() {
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(null, ReportTarget.MARETTA, "balenos_01", ReportKind.KILLED, minutes(0), null, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(testRegion.copy(enabled = false), ReportTarget.MARETTA, "grana_01", ReportKind.KILLED, minutes(0), null, now)
            }
            // KR에만 있는 채널·대상
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(testRegion, ReportTarget.MARETTA, "calpheon_01", ReportKind.KILLED, minutes(0), null, now)
            }
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(testRegion, ReportTarget.BHEG, "grana_01", ReportKind.SPOTTED, minutes(0), null, now)
            }
            // 다른 지역 고유 채널은 KR에서 거부
            assertFailsWith<InvalidArgumentException> {
                ReportHubRules.validate(kr, ReportTarget.MARETTA, "grana_01", ReportKind.KILLED, minutes(0), null, now)
            }
            // 이름이 같은 채널은 같은 코드로 두 지역 모두에서 유효
            assertEquals(
                "balenos_01",
                ReportHubRules.validate(testRegion, ReportTarget.MARETTA, "balenos_01", ReportKind.KILLED, minutes(0), null, now).channel,
            )
        }
    }

    @Nested
    inner class Channel {
        private val killedState = ChannelState(ReportKind.KILLED, minutes(0), 1, null)

        private fun decide(state: ChannelState?, kind: ReportKind, at: ZonedDateTime, contributed: Boolean = false) =
            ReportHubRules.decideChannel(state, kind, at, null, ReportMechanism.CHANNEL_RESPAWN, contributed)

        @Test
        fun `first report creates state`() {
            assertEquals(Decision.NEW_STATE, decide(null, ReportKind.KILLED, minutes(0)))
            assertEquals(Decision.NEW_STATE, decide(ChannelState(null, null, 0, null), ReportKind.KILLED, minutes(0)))
        }

        @Test
        fun `same kind within 5 minutes confirms`() {
            assertEquals(Decision.CONFIRM, decide(killedState, ReportKind.KILLED, minutes(4)))
            assertEquals(Decision.CONFIRM, decide(killedState, ReportKind.KILLED, minutes(-5)))
        }

        @Test
        fun `same reporter does not confirm twice`() {
            assertEquals(Decision.IGNORE, decide(killedState, ReportKind.KILLED, minutes(1), contributed = true))
        }

        @Test
        fun `same kind more than 5 minutes later is a new state`() {
            assertEquals(Decision.NEW_STATE, decide(killedState, ReportKind.KILLED, minutes(6)))
        }

        @Test
        fun `earlier report replaces only unverified state`() {
            assertEquals(Decision.NEW_STATE, decide(killedState, ReportKind.KILLED, minutes(-6)))
            assertEquals(Decision.IGNORE, decide(killedState.copy(confirmCount = 2), ReportKind.KILLED, minutes(-6)))
        }

        @Test
        fun `maretta spotted then killed then spotted`() {
            val spotted = ChannelState(ReportKind.SPOTTED, minutes(0), 1, null)
            // 이후 사망이 없으면 발견은 시간과 관계없이 동조
            assertEquals(Decision.CONFIRM, decide(spotted, ReportKind.SPOTTED, minutes(40)))
            assertEquals(Decision.NEW_STATE, decide(spotted, ReportKind.KILLED, minutes(10)))
            val killed = ChannelState(ReportKind.KILLED, minutes(10), 1, null)
            assertEquals(Decision.NEW_STATE, decide(killed, ReportKind.SPOTTED, minutes(70)))
        }

        @Test
        fun `buff overwrite is a new state`() {
            val active = ChannelState(ReportKind.ACTIVATED, minutes(-30), 2, null)
            assertEquals(
                Decision.NEW_STATE,
                ReportHubRules.decideChannel(active, ReportKind.ACTIVATED, minutes(0), null, ReportMechanism.BUFF, false),
            )
            assertEquals(
                Decision.CONFIRM,
                ReportHubRules.decideChannel(active, ReportKind.ACTIVATED, minutes(-27), null, ReportMechanism.BUFF, false),
            )
        }

        @Test
        fun `shared channel state from another session is replaced`() {
            val oldSession = ChannelState(ReportKind.KILLED, minutes(-60), 3, 1L)
            assertEquals(
                Decision.NEW_STATE,
                ReportHubRules.decideChannel(oldSession, ReportKind.SPOTTED, minutes(0), 2L, ReportMechanism.SHARED_SCHEDULE, false),
            )
        }

        @Test
        fun `replay after cancel rebuilds state with distinct reporters`() {
            val items = listOf(
                ChannelReplayItem(1, 100, ReportKind.KILLED, minutes(0)),
                ChannelReplayItem(2, 100, ReportKind.KILLED, minutes(1)),
                ChannelReplayItem(3, 200, ReportKind.KILLED, minutes(2)),
            )
            val result = ReportHubRules.replayChannel(items, ReportMechanism.CHANNEL_RESPAWN, null)
            assertNotNull(result)
            assertEquals(ReportKind.KILLED, result.kind)
            assertEquals(2, result.confirmCount)
            assertEquals(1, result.sourceReportId)

            assertNull(ReportHubRules.replayChannel(emptyList(), ReportMechanism.CHANNEL_RESPAWN, null))
        }

        @Test
        fun `replay after cancelling the source report keeps the remaining one`() {
            val remaining = listOf(ChannelReplayItem(3, 200, ReportKind.KILLED, minutes(2)))
            val result = ReportHubRules.replayChannel(remaining, ReportMechanism.CHANNEL_RESPAWN, null)!!
            assertEquals(3, result.sourceReportId)
            assertEquals(minutes(2), result.eventAt)
            assertEquals(1, result.confirmCount)
        }
    }

    @Nested
    inner class Session {
        private val lifetime = Duration.ofMinutes(30)
        private val current = SessionState(1, minutes(0))

        @Test
        fun `no session starts a new one`() {
            assertEquals(SessionDecision.NEW, ReportHubRules.decideSession(null, minutes(0), lifetime))
        }

        @Test
        fun `spawn within session window joins`() {
            assertEquals(SessionDecision.JOIN, ReportHubRules.decideSession(current, minutes(-10), lifetime))
            assertEquals(SessionDecision.JOIN, ReportHubRules.decideSession(current, minutes(30), lifetime))
        }

        @Test
        fun `spawn after lifetime starts a new session even before cooldown`() {
            // 쿨타임(12시간)보다 이르더라도 받는다 — 점검으로 쿨타임이 초기화될 수 있다
            assertEquals(SessionDecision.NEW, ReportHubRules.decideSession(current, minutes(31), lifetime))
        }

        @Test
        fun `spawn much earlier than anchor is ignored`() {
            assertEquals(SessionDecision.IGNORE, ReportHubRules.decideSession(current, minutes(-11), lifetime))
        }

        @Test
        fun `kill belongs to session until shortly after despawn`() {
            assertTrue(ReportHubRules.isKillWithinSession(current, minutes(35), lifetime))
            assertFalse(ReportHubRules.isKillWithinSession(current, minutes(36), lifetime))
            assertFalse(ReportHubRules.isKillWithinSession(null, minutes(0), lifetime))
        }
    }

    @Nested
    inner class Maintenance {
        @Test
        fun `start and open attach to one record in either order`() {
            val openOnly = MaintenanceState(1, null, minutes(0))
            assertEquals(
                MaintenanceDecision.Attach(1),
                ReportHubRules.decideMaintenance(listOf(openOnly), ReportKind.MAINTENANCE_START, minutes(-240)),
            )
            val startOnly = MaintenanceState(2, minutes(-240), null)
            assertEquals(
                MaintenanceDecision.Attach(2),
                ReportHubRules.decideMaintenance(listOf(startOnly), ReportKind.SERVER_OPEN, minutes(0)),
            )
        }

        @Test
        fun `report near existing time confirms`() {
            val record = MaintenanceState(1, minutes(-240), minutes(0))
            assertEquals(
                MaintenanceDecision.Confirm(1),
                ReportHubRules.decideMaintenance(listOf(record), ReportKind.SERVER_OPEN, minutes(20)),
            )
        }

        @Test
        fun `open before recorded start does not attach`() {
            val startOnly = MaintenanceState(2, minutes(0), null)
            assertEquals(
                MaintenanceDecision.New,
                ReportHubRules.decideMaintenance(listOf(startOnly), ReportKind.SERVER_OPEN, minutes(-60)),
            )
        }

        @Test
        fun `old records are not reused`() {
            val old = MaintenanceState(1, minutes(-60 * 30), null)
            assertEquals(MaintenanceDecision.New, ReportHubRules.decideMaintenance(listOf(old), ReportKind.SERVER_OPEN, minutes(0)))
        }

        @Test
        fun `regular day needs one reporter, other days need two`() {
            // now = KST 수요일
            assertTrue(ReportHubRules.isMaintenanceConfirmed(kr, null, now, 1))
            val thursday = now.plusDays(1)
            assertFalse(ReportHubRules.isMaintenanceConfirmed(kr, null, thursday, 1))
            assertTrue(ReportHubRules.isMaintenanceConfirmed(kr, null, thursday, 2))
            assertFalse(ReportHubRules.isMaintenanceConfirmed(kr, null, now, 0))
        }

        @Test
        fun `regular day is judged in region timezone`() {
            // 2026-11-04 16:00 UTC = KST 11-05(목) 01:00 → KR 정기점검일 아님
            val utcWednesdayKstThursday = ZonedDateTime.of(2026, 11, 4, 16, 0, 0, 0, ZoneOffset.UTC)
            assertEquals(DayOfWeek.WEDNESDAY, utcWednesdayKstThursday.dayOfWeek)
            assertFalse(ReportHubRules.isMaintenanceConfirmed(kr, null, utcWednesdayKstThursday, 1))
        }

        @Test
        fun `unknown regular day always needs two reporters`() {
            assertFalse(ReportHubRules.isMaintenanceConfirmed(testRegion, null, now, 1))
            assertTrue(ReportHubRules.isMaintenanceConfirmed(testRegion, null, now, 2))
        }
    }
}
