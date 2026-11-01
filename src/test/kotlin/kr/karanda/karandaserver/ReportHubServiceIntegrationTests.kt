package kr.karanda.karandaserver

import kr.karanda.karandaserver.dto.ReportHubReportRequest
import kr.karanda.karandaserver.entity.User
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportHubRelationType
import kr.karanda.karandaserver.enums.ReportKind
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.TooManyRequestsException
import kr.karanda.karandaserver.repository.jpa.ReportHubChannelStatusRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubMaintenanceRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReportRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubReporterRepository
import kr.karanda.karandaserver.repository.jpa.ReportHubSessionRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.service.ReportHubRelationService
import kr.karanda.karandaserver.service.ReportHubService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reporter Hub 서비스 통합 검증 (DB·Redis 필요).
 *
 * 실행: `SPRING_PROFILES_ACTIVE=develop ./gradlew integrationTest --tests "*ReportHubServiceIntegrationTests"`
 * develop 프로필의 로컬 H2 파일 DB를 쓰므로 bootRun 중인 서버가 있으면 먼저 멈춰야 한다(파일 잠금).
 *
 * 테스트가 쓰는 대상·채널의 Reporter Hub 데이터는 앞뒤로 지운다(로컬 개발 DB 전용).
 */
@Tag("integration")
@SpringBootTest
class ReportHubServiceIntegrationTests {
    @Autowired private lateinit var service: ReportHubService
    @Autowired private lateinit var relationService: ReportHubRelationService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var reportRepository: ReportHubReportRepository
    @Autowired private lateinit var sessionRepository: ReportHubSessionRepository
    @Autowired private lateinit var channelStatusRepository: ReportHubChannelStatusRepository
    @Autowired private lateinit var maintenanceRepository: ReportHubMaintenanceRepository
    @Autowired private lateinit var reporterRepository: ReportHubReporterRepository

    private val region = BDORegion.KR
    private val createdUsers = mutableListOf<User>()

    @BeforeEach
    fun clean() = cleanReportHubData()

    @AfterEach
    fun tearDown() {
        cleanReportHubData()
        createdUsers.forEach {
            relationService.onUserDeleted(it.id!!)
            userRepository.delete(it)
        }
        createdUsers.clear()
    }

    private fun cleanReportHubData() {
        val targets = setOf(ReportTarget.MARETTA, ReportTarget.SUN, ReportTarget.WHALE)
        reportRepository.findAll().filter { it.region == region && (it.target in targets || it.target == null) }
            .let { reportRepository.deleteAll(it) }
        channelStatusRepository.findAllByRegion(region).filter { it.target in targets }
            .let { channelStatusRepository.deleteAll(it) }
        sessionRepository.findAll().filter { it.region == region && it.target == ReportTarget.WHALE }
            .let { sessionRepository.deleteAll(it) }
        maintenanceRepository.findAll().filter { it.region == region }.let { maintenanceRepository.deleteAll(it) }
    }

    private fun newUser(): User {
        val suffix = UUID.randomUUID().toString().take(8)
        val user = userRepository.save(
            User(
                userUUID = UUID.randomUUID().toString(),
                discordId = "rh-test-$suffix",
                discordDiscriminator = "0",
                userName = "rh-test-$suffix",
                avatarHash = null,
            )
        )
        createdUsers += user
        return user
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))

    private fun report(user: User, target: ReportTarget?, channel: String?, kind: ReportKind, remaining: Int? = null) =
        service.addReport(
            user.userUUID,
            ReportHubReportRequest(
                region = region,
                target = target,
                channel = channel,
                kind = kind,
                eventAt = if (remaining == null) now() else null,
                remainingMinutes = remaining,
            ),
        )

    @Test
    fun `maretta kill is confirmed by another reporter and appears in snapshot`() {
        val a = newUser()
        val b = newUser()
        val first = report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)
        val created = first.status.channels.single()
        assertEquals(ReportKind.KILLED, created.kind)
        assertEquals(1, created.confirmCount)
        assertEquals(a.id, created.reporterId)
        assertTrue(first.status.reporters.any { it.id == a.id && it.reportCount == 1L })

        val second = report(b, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)
        val confirmed = second.status.channels.single()
        assertEquals(2, confirmed.confirmCount)
        assertEquals(a.id, confirmed.reporterId, "동조는 원래 제보자를 유지한다")
        assertTrue(confirmed.version > created.version, "버전이 증가해야 클라이언트가 병합한다")

        val snapshot = service.getStatus(region)
        assertEquals(2, snapshot.channels.single { it.target == ReportTarget.MARETTA && it.channel == "balenos_03" }.confirmCount)
    }

    @Test
    fun `same reporter cannot report the same key twice within a minute`() {
        val a = newUser()
        report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.SPOTTED)
        assertFailsWith<TooManyRequestsException> { report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.SPOTTED) }
    }

    @Test
    fun `cancel recomputes state from remaining reports`() {
        val a = newUser()
        val b = newUser()
        val first = report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)
        report(b, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)

        val delta = service.cancelReport(a.userUUID, first.report.id, "mistake")
        val status = delta.channels.single()
        assertEquals(1, status.confirmCount)
        assertEquals(b.id, status.reporterId, "남은 제보가 새 원본이 된다")

        val bReport = reportRepository.findTop20ByRegionAndReporterIdOrderByReportedAtDesc(region, b.id!!).single()
        val cleared = service.cancelReport(b.userUUID, bReport.id!!, "")
        assertNull(cleared.channels.single().kind, "유효 제보가 없으면 정보 없음")
    }

    @Test
    fun `buff start is derived from remaining minutes`() {
        val a = newUser()
        val result = report(a, ReportTarget.SUN, "mediah_03", ReportKind.ACTIVATED, remaining = 40)
        val status = result.status.channels.single()
        val minutesAgo = java.time.Duration.between(status.eventAt!!.toInstant(), now().toInstant()).toMinutes()
        assertTrue(minutesAgo in 19..21, "남은 40분 → 약 20분 전 시작 (실제 $minutesAgo)")
    }

    @Test
    fun `shared spawns on different channels join one session`() {
        val a = newUser()
        val b = newUser()
        val first = report(a, ReportTarget.WHALE, "serendia_02", ReportKind.SPOTTED)
        val session = first.status.sessions.single()
        assertEquals(1, session.confirmCount)

        val second = report(b, ReportTarget.WHALE, "serendia_03", ReportKind.SPOTTED)
        val joined = second.status.sessions.single()
        assertEquals(session.id, joined.id)
        assertEquals(2, joined.confirmCount)
        assertEquals(joined.id, second.status.channels.single().sessionId)

        val killed = report(a, ReportTarget.WHALE, "serendia_02", ReportKind.KILLED)
        assertEquals(ReportKind.KILLED, killed.status.channels.single().kind)
    }

    @Test
    fun `maintenance start and open attach to one record`() {
        val a = newUser()
        val b = newUser()
        val start = report(a, null, null, ReportKind.MAINTENANCE_START)
        val record = start.status.maintenances.single()
        assertNotNull(record.startedAt)

        val open = report(b, null, null, ReportKind.SERVER_OPEN)
        val attached = open.status.maintenances.single()
        assertEquals(record.id, attached.id)
        assertNotNull(attached.endedAt)
        // 서로 다른 제보자 2명이면 요일과 관계없이 확정
        assertTrue(attached.confirmed)

        val isRegularDay = now().withZoneSameInstant(ZoneId.of("Asia/Seoul")).dayOfWeek == DayOfWeek.WEDNESDAY
        assertEquals(isRegularDay, record.confirmed, "제보 1건은 KST 수요일에만 확정")
    }

    @Test
    fun `follow and block update follower count`() {
        val a = newUser()
        val b = newUser()
        report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)

        val followed = relationService.setRelation(b.userUUID, region, a.id!!, ReportHubRelationType.FOLLOW)
        assertEquals(1, followed.reporter!!.followerCount)
        assertEquals(1, reporterRepository.findByUserIdAndRegion(a.id!!, region)!!.followerCount)

        relationService.setRelation(b.userUUID, region, a.id!!, ReportHubRelationType.BLOCK)
        assertEquals(0, reporterRepository.findByUserIdAndRegion(a.id!!, region)!!.followerCount)
        assertEquals(ReportHubRelationType.BLOCK, relationService.getRelations(b.userUUID, region).single().type)

        relationService.removeRelation(b.userUUID, region, a.id!!)
        assertTrue(relationService.getRelations(b.userUUID, region).isEmpty())
        assertEquals(0, reporterRepository.findByUserIdAndRegion(a.id!!, region)!!.followerCount)
    }

    @Test
    fun `deleted user's follows are removed and counts restored`() {
        val a = newUser()
        val b = newUser()
        report(a, ReportTarget.MARETTA, "balenos_03", ReportKind.KILLED)
        relationService.setRelation(b.userUUID, region, a.id!!, ReportHubRelationType.FOLLOW)

        relationService.onUserDeleted(b.id!!)
        assertEquals(0, reporterRepository.findByUserIdAndRegion(a.id!!, region)!!.followerCount)
        relationService.onUserDeleted(a.id!!)
        assertNull(reporterRepository.findByUserIdAndRegion(a.id!!, region))
    }
}
