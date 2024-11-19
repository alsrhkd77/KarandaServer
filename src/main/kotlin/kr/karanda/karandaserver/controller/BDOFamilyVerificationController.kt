package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.BDOFamily
import kr.karanda.karandaserver.dto.BDOFamilyVerificationRequest
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.service.BDOFamilyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@Tag(name = "BDO family verification", description = "BDO family verification API")
@RestController
@RequestMapping("/auth/bdo-family")
class BDOFamilyVerificationController(val bdoFamilyService: BDOFamilyService) {

    @PostMapping("/register")
    @Operation(summary = "Register new BDO family")
    fun registerFamily(@RequestBody data: BDOFamilyVerificationRequest): BDOFamily {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        return bdoFamilyService.startVerification(
            uuid = authentication.userUUID,
            familyName = data.familyName,
            region = data.region,
            code = data.code,
        )
        //TODO: 시작 성공, 올바르지 않은 닉네임, 올바르지 않은 코드(=파싱 실패), 이미 사용중인 코드(->빼앗기 위해 계속 진행)
    }

    @PatchMapping("/verify")
    @Operation(summary = "Verify BDO family from official web site")
    fun familyVerify(@RequestBody data: BDOFamilyVerificationRequest): BDOFamily {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        println(data.familyName)
        return bdoFamilyService.verifyFamily(
            uuid = authentication.userUUID,
            region = data.region,
            code = data.code,
        )
    }

    @DeleteMapping("/unregister")
    @Operation(summary = "Unregister BDO family")
    fun unregisterFamily(@RequestBody data: BDOFamilyVerificationRequest): ResponseEntity<Void> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        bdoFamilyService.deleteFamily(
            uuid = authentication.userUUID,
            region = data.region,
            code = data.code,
        )
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @GetMapping("/families")
    @Operation(summary = "Get all BDO families registered in Karanda")
    fun getAllFamilies(): List<BDOFamily> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        return bdoFamilyService.getAllFamily(authentication.userUUID)
    }

    @PatchMapping("/set-main")
    @Operation(summary = "Change the current user's main family")
    fun updateMainFamily(@RequestBody data: BDOFamilyVerificationRequest): ResponseEntity<Void> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        bdoFamilyService.changeMainFamily(
            uuid = authentication.userUUID,
            region = data.region,
            code = data.code,
        )
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @PatchMapping("/refresh")
    @Operation(summary = "Refresh specific family data")
    fun refresh(@RequestBody data: BDOFamilyVerificationRequest): BDOFamily{
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        return bdoFamilyService.refreshFamilyData(
            uuid = authentication.userUUID,
            region = data.region,
            code = data.code,
        )
    }
}