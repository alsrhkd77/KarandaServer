package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.ReportDTO
import kr.karanda.karandaserver.dto.ReportStatusDTO
import kr.karanda.karandaserver.entity.Report
import kr.karanda.karandaserver.entity.ReportStatus
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.repository.jpa.ReportStatusRepository
import kr.karanda.karandaserver.repository.jpa.ReportRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.isBetween
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class ReportHubService(
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    private val reportStatusRepository: ReportStatusRepository,
) {
    private val independent = listOf(   // 각 채널별 독립 상태를 가지는 것들
        ReportTarget.SUN,
        ReportTarget.MOON,
        ReportTarget.EARTH,
        ReportTarget.GOLDEN_BELL
    )

    fun getAllStatus(region: BDORegion): List<ReportStatusDTO> {
        return reportStatusRepository.findAllByRegion(region).map { it.toDTO() }
    }

    fun getTargetReports(region: BDORegion, target: ReportTarget): List<ReportDTO> {
        val isIndependent = independent.contains(target)
        if (isIndependent) {
            return reportStatusRepository.findAllByTargetAndRegion(
                target = target,
                region = region
            ).filter { it.sourceReport != null }.map { it.sourceReport!!.toDTO() }
        } else {
            val status = reportStatusRepository.findByTargetAndRegionAndChannel(
                target = target,
                region = region,
                channel = null
            )
            if(status != null) {
                return reportRepository.findAllByTargetAndRegionAndEventAtAfterOrderByEventAtAsc(
                    target = target,
                    region = region,
                    eventAt = status.eventAt
                ).map { it.toDTO() }
            }
        }
        return listOf()
    }

    fun getUserReports(){

    }

    fun addReport(userUUID: String, reportDTO: ReportDTO): ReportDTO {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        val report = reportRepository.save(
            Report(
                target = reportDTO.target,
                region = reportDTO.region,
                channel = reportDTO.channel,
                active = reportDTO.active,
                eventAt = reportDTO.eventAt,
                reportedAt = ZonedDateTime.now(ZoneId.of("UTC")),
                reporter = user
            )
        )
        val isIndependent = independent.contains(report.target)
        var currentStatus = reportStatusRepository.findByTargetAndRegionAndChannel(
            target = report.target,
            region = report.region,
            channel = if (isIndependent) report.channel else null
        )
        if (currentStatus == null) {
            currentStatus = reportStatusRepository.save(
                ReportStatus(
                    target = report.target,
                    region = report.region,
                    channel = if (isIndependent) report.channel else null,
                    active = report.active,
                    eventAt = report.eventAt,
                    updatedAt = report.reportedAt,
                    sourceReport = report
                )
            )
        } else if (isIndependent || report.active != currentStatus.active || !report.eventAt.isBetween(
                currentStatus.eventAt,
                currentStatus.eventAt.plusHours(1)
            )
        ) {
            currentStatus = updateReportStatus(currentStatus, report)
        } else {
            return report.toDTO()
        }
        processNotify(currentStatus, isIndependent)
        return report.toDTO()
    }

    fun cancelReport(userUUID: String, reportId: Long, reason: String): ReportDTO {
        val report =
            reportRepository.findByIdAndReporterUserUUIDAndIsCanceledFalse(
                id = reportId,
                userUUID = userUUID,
            ) ?: throw InvalidArgumentException()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        if (now.isAfter(report.reportedAt.plusMinutes(3))) {
            throw InvalidArgumentException()    //TODO:취소 가능 시간 초과
        }
        reportRepository.save(report.apply {
            isCanceled = true
            cancelReason = reason
        })
        val isIndependent = independent.contains(report.target)
        reportStatusRepository.findBySourceReportId(reportId)?.let { currentStatus ->
            val latest: Report? =
                if (isIndependent) reportRepository.findTop1ByTargetAndRegionAndChannelAndIsCanceledFalseOrderByEventAtDesc(
                    target = report.target,
                    region = report.region,
                    channel = report.channel
                ) else findLatestReportWithinOneHour(
                    target = report.target,
                    region = report.region,
                )

            val status = if (latest == null) {
                reportStatusRepository.save(currentStatus.apply {
                    active = false
                    eventAt = now
                    updatedAt = now
                    sourceReport = null
                })
            } else {
                updateReportStatus(currentStatus, latest)
            }
            processNotify(status, isIndependent)
        }
        return report.toDTO()
    }

    private fun updateReportStatus(
        currentStatus: ReportStatus,
        report: Report,
    ): ReportStatus {
        return reportStatusRepository.save(
            currentStatus.apply {
                active = report.active
                eventAt = report.eventAt
                updatedAt = report.reportedAt
                sourceReport = report
            }
        )
    }

    private fun processNotify(status: ReportStatus, isIndependent: Boolean) {
        //추후 세부 동작 구현
        val data = status.toDTO()
        if (!isIndependent) {
            println("[data]를 가공해 FCM 알림 보내기")
        }
        println("[data]를 가공해 websocket으로 보내기")
    }

    private fun findLatestReportWithinOneHour(target: ReportTarget, region: BDORegion): Report? {
        val latest =
            reportRepository.findTop1ByTargetAndRegionAndIsCanceldFalseOrderByEventAtDesc(target, region) ?: return null
        return reportRepository.findTop1ByTargetAndRegionAndActiveAndEventAtBetweenAndIsCanceldFalseOrderByEventAtAsc(
            target = target,
            region = region,
            active = latest.active,
            eventAtStart = latest.eventAt.minusHours(1),
            eventAtEnd = latest.eventAt
        )
    }
}