package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kr.karanda.karandaserver.data.AuthorizationResponse
import kr.karanda.karandaserver.data.Tokens
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.service.DiscordService
import kr.karanda.karandaserver.service.UserService
import kr.karanda.karandaserver.util.TokenFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView

@Tag(name = "Discord Auth", description = "Discord Auth API")
@RestController
@RequestMapping("/auth/discord")
class DiscordAuthController(
    val tokenFactory: TokenFactory,
    val discordService: DiscordService,
    val userService: UserService,
    val environment: Environment,
) {
    private fun authenticate(code: String, redirectURL: String): Tokens {
        val discordToken = discordService.exchangeCode(code = code, redirectUrl = redirectURL)
        val discordUser = discordService.getUserDataByToken(discordToken)
        var user = userService.getByDiscordId(discordUser.id)
        if (user == null) {
            user = userService.createUser(
                discordId = discordUser.id,
                username = discordUser.username,
                avatar = discordUser.avatar
            )
        }
        return tokenFactory.createTokens(userUUID = user.userUUID, username = user.username)
    }

    @GetMapping("/authenticate/windows")
    @Operation(summary = "Authentication from Karanda windows client")
    fun authenticationFromWindows(code: String, request: HttpServletRequest): ModelAndView {
        val (accessToken, refreshToken) = authenticate(code, request.requestURL.toString())
        val frontUrl = "http://localhost:8082"
        return ModelAndView("redirect:$frontUrl?token=$accessToken&&refresh-token=$refreshToken")
    }

    @GetMapping("/authenticate/web")
    @Operation(summary = "Authentication from Karanda web client")
    fun authenticationFromWeb(code: String, request: HttpServletRequest): ModelAndView {
        val (accessToken, refreshToken) = authenticate(code, request.requestURL.toString())
        var frontUrl = "http://localhost:2345"
        if (environment.activeProfiles.contains("production")) {
            frontUrl = "https://www.karanda.kr"
        }
        frontUrl += "/#/auth/authenticate"
        return ModelAndView("redirect:$frontUrl?token=$accessToken&&refresh-token=$refreshToken")
    }

    @GetMapping("/authorization")
    @Operation(summary = "Authorization with access token")
    fun authorization(): AuthorizationResponse {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return authorizationByAuthentication(authentication, withToken = false)
    }

    @GetMapping("/refresh")
    @Operation(summary = "Exchanging old tokens for new tokens")
    fun refreshAccessToken(): AuthorizationResponse {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        return authorizationByAuthentication(authentication, withToken = true)
    }

    private fun authorizationByAuthentication(authentication: TokenClaims, withToken: Boolean): AuthorizationResponse {
        val response = AuthorizationResponse()
        val user = userService.getUserEntityByUUID(authentication.userUUID)
        val userData = discordService.getUserDataById(user.discordId)
        if (userData.avatar != null) {
            response.avatar = "${userData.id}/${userData.avatar}.png"
            if (userData.avatar != user.avatarHash) {
                user.avatarHash = userData.avatar
            }
        }
        response.username = userData.username
        response.discordId = userData.id
        if (userData.username != user.userName) {
            user.userName = userData.username
        }
        userService.updateUserFromEntity(user)
        if (withToken) {
            tokenFactory.createTokens(userUUID = user.userUUID, username = userData.username).apply {
                response.token = this.accessToken
                response.refreshToken = this.refreshToken
            }
        }
        return response
    }

    @DeleteMapping("/unregister")
    fun unregister(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication.principal as TokenClaims
        userService.deleteUserByUUID(authentication.userUUID)
        return ResponseEntity(HttpStatus.OK)
    }
}