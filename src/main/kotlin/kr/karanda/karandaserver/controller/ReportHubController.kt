package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.ReportHubPolicyDTO
import kr.karanda.karandaserver.dto.ReportHubRelationDTO
import kr.karanda.karandaserver.dto.ReportHubRelationRequest
import kr.karanda.karandaserver.dto.ReportHubReportListDTO
import kr.karanda.karandaserver.dto.ReportHubReportRequest
import kr.karanda.karandaserver.dto.ReportHubReportResultDTO
import kr.karanda.karandaserver.dto.ReportHubStatusDTO
import kr.karanda.karandaserver.dto.ReporterProfileDTO
import kr.karanda.karandaserver.dto.ReporterSummaryDTO
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import kr.karanda.karandaserver.service.ReportHubRelationService
import kr.karanda.karandaserver.service.ReportHubService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Reporter Hub API.
 *
 * 조회(GET regions·policy·status·reports·reporters)는 SecurityConfiguration whitePath에 포함되어 로그인이 필요 없다
 * (production에서는 Qualification 헤더는 여전히 필요). 제보·취소·관계는 로그인이 필요하다.
 */
@Tag(name = "Report Hub", description = "Report Hub API")
@RestController
@RequestMapping("/report-hub")
class ReportHubController(
    private val reportHubService: ReportHubService,
    private val relationService: ReportHubRelationService,
) {
    @GetMapping("/regions")
    @Operation(summary = "Regions where Report Hub is enabled")
    @SecurityRequirement(name = "Qualifications")
    fun getRegions(): List<BDORegion> = reportHubService.getEnabledRegions()

    @GetMapping("/policy")
    @Operation(summary = "Targets, channels and rules of the region. enabled=false if the region is not supported yet")
    @SecurityRequirement(name = "Qualifications")
    fun getPolicy(@RequestParam(required = true) region: BDORegion): ReportHubPolicyDTO =
        reportHubService.getPolicy(region)

    @GetMapping("/status")
    @Operation(summary = "Status snapshot of the region. Merge with realtime deltas by each row's version")
    @SecurityRequirement(name = "Qualifications")
    fun getStatus(@RequestParam(required = true) region: BDORegion): ReportHubStatusDTO =
        reportHubService.getStatus(region)

    @GetMapping("/reports")
    @Operation(summary = "Recent 20 reports of the target and channel")
    @SecurityRequirement(name = "Qualifications")
    fun getReports(
        @RequestParam(required = true) region: BDORegion,
        @RequestParam(required = true) target: ReportTarget,
        @RequestParam(required = true) channel: String,
    ): ReportHubReportListDTO = reportHubService.getReports(region, target, channel)

    @PostMapping("/report")
    @Operation(summary = "Create new report. Returns the report and the status delta caused by it")
    fun addReport(@RequestBody request: ReportHubReportRequest): ResponseEntity<ReportHubReportResultDTO> {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return ResponseEntity(reportHubService.addReport(authentication.userUUID, request), HttpStatus.CREATED)
    }

    @PatchMapping("/cancel")
    @Operation(summary = "Cancel own report within 10 minutes. Not deleting it. Returns the status delta")
    fun cancelReport(
        @RequestParam(required = true) reportId: Long,
        @RequestParam(required = false, defaultValue = "") reason: String,
    ): ReportHubStatusDTO {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return reportHubService.cancelReport(authentication.userUUID, reportId, reason)
    }

    @GetMapping("/reporters")
    @Operation(summary = "Reporter leaderboard of the region ordered by reports in recent 7 days")
    @SecurityRequirement(name = "Qualifications")
    fun getReporters(@RequestParam(required = true) region: BDORegion): List<ReporterSummaryDTO> =
        reportHubService.getReporters(region)

    @GetMapping("/reporters/{reporterId}")
    @Operation(summary = "Reporter profile and recent reports in the region")
    @SecurityRequirement(name = "Qualifications")
    fun getReporter(
        @PathVariable reporterId: Long,
        @RequestParam(required = true) region: BDORegion,
    ): ReporterProfileDTO = reportHubService.getReporterProfile(region, reporterId)

    @GetMapping("/relations")
    @Operation(summary = "My follows and blocks in the region")
    fun getRelations(@RequestParam(required = true) region: BDORegion): List<ReportHubRelationDTO> {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return relationService.getRelations(authentication.userUUID, region)
    }

    @PutMapping("/relations/{reporterId}")
    @Operation(summary = "Follow or block a reporter in the region")
    fun setRelation(
        @PathVariable reporterId: Long,
        @RequestParam(required = true) region: BDORegion,
        @RequestBody request: ReportHubRelationRequest,
    ): ReportHubRelationDTO {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return relationService.setRelation(authentication.userUUID, region, reporterId, request.type)
    }

    @DeleteMapping("/relations/{reporterId}")
    @Operation(summary = "Remove follow or block")
    fun removeRelation(
        @PathVariable reporterId: Long,
        @RequestParam(required = true) region: BDORegion,
    ): ResponseEntity<HttpStatus> {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        relationService.removeRelation(authentication.userUUID, region, reporterId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
