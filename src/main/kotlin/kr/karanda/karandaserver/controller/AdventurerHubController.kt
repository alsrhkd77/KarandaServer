package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.data.SimplifiedRecruitmentPost
import kr.karanda.karandaserver.dto.Applicant
import kr.karanda.karandaserver.dto.RecruitmentPost
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.service.AdventurerHubService
import org.apache.coyote.BadRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@Tag(name = "Adventurer hub", description = "Adventurer hub API")
@RestController
@RequestMapping("/adventurer-hub")
class AdventurerHubController(val adventurerHubService: AdventurerHubService) {

    @PostMapping("post/create")
    @Operation(summary = "Create new recruitment post")
    fun createNewPost(@RequestBody data: RecruitmentPost): ResponseEntity<Long> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        if (data.title.isEmpty() || data.title.length > 40) {
            throw BadRequestException("bad title")
        } else if (data.category == "guildWarHeroes") {
            if (data.guildName.isNullOrEmpty()) {
                throw BadRequestException("bad guild")
            }
            data.guildName = data.guildName!!.replace(Regex(" "), "")
        }
        if (data.discordLink != null && data.discordLink!!.isNotEmpty()) {
            if (!data.discordLink!!.startsWith("https://discord.gg/")) {
                if (data.discordLink!!.contains(Regex("^(http://|https://)"))) {
                    data.discordLink = ""
                } else {
                    data.discordLink = "https://discord.gg/${data.discordLink}"
                }
            }
        }
        //TODO: 링크 공백 제거, 디스코드 링크 검증
        val postId: Long = adventurerHubService.createNewPost(uuid = authentication.userUUID, data = data).id!!
        return ResponseEntity(postId, HttpStatus.CREATED)
    }

    @PatchMapping("post/update")
    @Operation(summary = "Update recruitment post")
    fun updatePost(@RequestBody data: RecruitmentPost): ResponseEntity<RecruitmentPost> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.updatePost(uuid = authentication.userUUID, data = data)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/posts")
    @Operation(summary = "Get 100 recent recruitment posts. Response is a list of simplified posts")
    fun getPosts(@RequestParam(name = "region", required = true) region: String): List<SimplifiedRecruitmentPost> {
        return adventurerHubService.getPosts(region).map { it.simplify() }
    }

    @GetMapping("/post")
    @Operation(summary = "Get recruitment post's information.")
    fun getPost(@RequestParam(required = true) postId: Long): RecruitmentPost {
        return adventurerHubService.getPost(postId = postId)
    }

    @GetMapping("/post/detail")
    @Operation(summary = "Get recruitment post's detail")
    fun getDetail(@RequestParam(required = true) postId: Long): RecruitmentPost {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return adventurerHubService.getPost(postId = postId, uuid = authentication.userUUID)
    }

    @PostMapping("/post/open")
    @Operation(summary = "Start recruiting for this post")
    fun openPost(@RequestParam(required = true) postId: Long): ResponseEntity<RecruitmentPost> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.openPost(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/post/close")
    @Operation(summary = "Recruiting for this post is closed")
    fun closePost(@RequestParam(required = true) postId: Long): ResponseEntity<RecruitmentPost> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.closePost(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/post/apply")
    @Operation(summary = "Apply to this post")
    fun applyToPost(@RequestParam(required = true) postId: Long): ResponseEntity<Applicant> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.applyToPost(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/post/cancel")
    @Operation(summary = "Cancel to this post")
    fun cancelToPost(@RequestParam(required = true) postId: Long): ResponseEntity<Applicant> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.cancelToPost(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/post/approve")
    @Operation(summary = "Approve the applicant")
    fun approveApplicant(@RequestParam(required = true) postId: Long, @RequestParam(required = true) applicantId: String): ResponseEntity<Applicant> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.approveApplicant(
            postId = postId,
            applicantId = applicantId,
            uuid = authentication.userUUID
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/post/reject")
    @Operation(summary = "Reject the applicant")
    fun rejectApplicant(
        @RequestParam(required = true) postId: Long,
        @RequestParam(required = true) applicantId: String,
        @RequestParam(required = true) reason: String
    ): ResponseEntity<Applicant> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.rejectApplicant(
            postId = postId,
            applicantId = applicantId,
            reason = reason,
            uuid = authentication.userUUID
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/post/applicant")
    @Operation(summary = "Get single applicant")
    fun getPostApplicant(@RequestParam postId: Long): ResponseEntity<Applicant?> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.getPostApplicant(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/post/applicants")
    @Operation(summary = "Get applicants list")
    fun getPostApplicants(@RequestParam postId: Long): ResponseEntity<List<Applicant>> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.getPostApplicants(postId = postId, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/user/applied")
    @Operation(summary = "Get list of user applied")
    fun getUserApplied(): ResponseEntity<List<Applicant>>{
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = adventurerHubService.getUserApplied(uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }
}