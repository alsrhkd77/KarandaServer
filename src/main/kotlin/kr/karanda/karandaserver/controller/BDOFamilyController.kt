package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.*
import kr.karanda.karandaserver.service.BDOFamilyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "BDO Family", description = "BDO Family API")
@RestController
@RequestMapping("/bdo-family")
class BDOFamilyController(private val familyService: BDOFamilyService) {
    @PostMapping("/register")
    @Operation(summary = "Create new family with profile code & region")
    fun registerNewFamily(param: BDOFamilyRequest): BDOFamilyDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.registerNewFamily(authentication.userUUID, param.code, param.region)
    }

    @PostMapping("/start-verification")
    @Operation(summary = "Start family verification")
    fun startVerification(param: BDOFamilyRequest): BDOFamilyVerificationDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.startVerification(authentication.userUUID, param.code, param.region)
    }

    @PostMapping("/verify")
    @Operation(summary = "Try verification")
    fun verifyFamily(param: BDOFamilyRequest): BDOFamilyDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.verifyFamily(authentication.userUUID, param.code, param.region)
    }

    @DeleteMapping("/unregister")
    @Operation(summary = "Delete family")
    fun unregisterFamily(param: BDOFamilyRequest): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        familyService.unregisterFamily(authentication.userUUID, param.code, param.region)
        return ResponseEntity(HttpStatus.OK)
    }

    @PostMapping("/set-main-family")
    @Operation(summary = "Set family to main")
    fun setMainFamily(param: BDOFamilyRequest): UserDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.setMainFamily(authentication.userUUID, param.code, param.region)
    }

    @PostMapping("/update-family")
    @Operation(summary = "Update family data")
    fun updateFamilyData(param: BDOFamilyRequest): BDOFamilyDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.updateFamilyData(authentication.userUUID, param.code, param.region)
    }

    @GetMapping("/families")
    @Operation(summary = "Get families")
    fun getFamilies(): List<BDOFamilyDTO> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return familyService.getFamilies(authentication.userUUID)
    }
}