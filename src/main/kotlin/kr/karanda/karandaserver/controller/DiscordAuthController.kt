package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kr.karanda.karandaserver.dto.RefreshTokenResponse
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.dto.UserDTO
import kr.karanda.karandaserver.service.AuthService
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView

@Tag(name = "Discord Auth", description = "Discord Auth API")
@RestController
@RequestMapping("/auth/discord")
class DiscordAuthController(
    val authService: AuthService,
    val environment: Environment,
) {
    @GetMapping("/authenticate/windows")
    @Operation(summary = "Authentication from Karanda windows client")
    fun authenticationFromWindows(code: String, request: HttpServletRequest): ModelAndView {
        val (accessToken, refreshToken) = authService.authenticate(code, request.requestURL.toString())
        val frontUrl = "http://localhost:8082"
        return ModelAndView("redirect:$frontUrl?token=$accessToken&&refresh-token=$refreshToken")
    }

    @GetMapping("/authenticate/web")
    @Operation(summary = "Authentication from Karanda web client")
    fun authenticationFromWeb(code: String, request: HttpServletRequest): ModelAndView {
        val (accessToken, refreshToken) = authService.authenticate(code, request.requestURL.toString())
        var frontUrl = "http://localhost:2345"
        if (environment.activeProfiles.contains("production")) {
            frontUrl = "https://www.karanda.kr"
        }
        frontUrl += "/auth/authenticate"
        return ModelAndView("redirect:$frontUrl?token=$accessToken&&refresh-token=$refreshToken")
    }

    @GetMapping("/authorization")
    @Operation(summary = "Authorization with access token")
    fun authorization(): UserDTO {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return authService.authorization(authentication.userUUID)
    }

    @GetMapping("/refresh")
    @Operation(summary = "Exchanging old tokens for new tokens")
    fun refreshAccessToken(@RequestHeader headers: Map<String, String>): RefreshTokenResponse {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        val tokens = authService.createTokens(userUUID = authentication.userUUID)
        val user = authService.authorization(authentication.userUUID)
        return RefreshTokenResponse(user = user, tokens = tokens)
        //TODO: 유저 데이터 없이 토큰만 반환하도록 수정 필요(프론트랑 같이 수정해야함)
        // 프론트에서 토큰을 http 클라이언트에서 관리하게되면 어차피 사용자 정보는 재요청 하게됨
    }

    @DeleteMapping("/unregister")
    fun unregister(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        authService.unregister(authentication.userUUID)
        return ResponseEntity(HttpStatus.OK)
    }
}
