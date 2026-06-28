package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.ReportDTO
import kr.karanda.karandaserver.dto.ReportStatusDTO
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.service.ReportHubService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@Tag(name = "Report Hub", description = "Report Hub API")
@RestController
@RequestMapping("/report-hub")
class ReportHubController(
    private val reportHubService: ReportHubService
) {
    @GetMapping("/status")
    fun getStatus(@RequestParam(required = true) region: BDORegion): List<ReportStatusDTO> {
        return reportHubService.getAllStatus(region = region)
    }

    @GetMapping("/reports")
    fun getReports() {
    }

    @PostMapping("/report")
    @Operation(summary = "Create new Report")
    fun addReport(@RequestBody param: ReportDTO): ReportDTO {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return reportHubService.addReport(authentication.userUUID, param)
    }

    @PatchMapping("/cancel")
    @Operation(summary = "Canceling previous report. Not deleting it.")
    fun cancelReport(@RequestParam(required = true) reportId: Long, @RequestParam reason: String = ""): ReportDTO {
        val authentication = SecurityContextHolder.getContext().authentication!!.principal as TokenClaims
        return reportHubService.cancelReport(authentication.userUUID, reportId, reason)
    }
}