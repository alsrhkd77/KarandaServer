package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.dto.UserFcmSettingsDTO
import kr.karanda.karandaserver.service.FcmService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "FCM", description = "Manage user fcm settings")
@RestController
@RequestMapping("/fcm")
class FcmController(private val fcmService: FcmService) {
    @GetMapping("/settings")
    @Operation(summary = "Get user's fcm settings")
    fun getFcmSettings(@RequestParam(required = true) token: String): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = fcmService.getUserFcmSettings(token = token, uuid = authentication.userUUID)
        return if (result == null) {
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        } else {
            ResponseEntity.ok(result)
        }
    }

    @PostMapping("/settings/save")
    @Operation(summary = "Create & Update user's fcm settings")
    fun saveFcmSettings(@RequestBody data: UserFcmSettingsDTO): ResponseEntity<UserFcmSettingsDTO> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val result = fcmService.saveUserFcmSettings(userFcmSettings = data, uuid = authentication.userUUID)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/token/delete")
    @Operation(summary = "Delete user's fcm token")
    fun deleteFcmToken(@RequestParam(required = true) token: String): ResponseEntity<HttpStatus> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        fcmService.unregisterFcmToken(token = token, uuid = authentication.userUUID)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PatchMapping("/token/update")
    @Operation(summary = "Update user's fcm token")
    fun updateFcmToken(
        @RequestParam(required = true) oldToken: String,
        @RequestParam(required = true) newToken: String
    ): ResponseEntity<HttpStatus> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        fcmService.updateFcmToken(oldToken = oldToken, newToken = newToken, uuid = authentication.userUUID)
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}