package kr.karanda.karandaserver.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import kr.karanda.karandaserver.dto.properties.TokenProperties
import kr.karanda.karandaserver.dto.Tokens
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.repository.DefaultDataProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import javax.crypto.spec.SecretKeySpec

/**
 * JWT 생성 및 검증을 위한 클래스
 */
@Component
class TokenUtils(private val defaultDataProvider: DefaultDataProvider) {

    val logger: Logger = LoggerFactory.getLogger(TokenUtils::class.java)

    val tokenProperties: TokenProperties
        get() = defaultDataProvider.getTokenProperties()

    /**
     * 유저 정보를 이용해 **AccessToken**과 **RefreshToken**을 모두 생성.
     *
     * @param [userUUID]
     * @param [username]
     * @return [Tokens] **AccessToken**, **RefreshToken**
     * @see [createAccessToken]
     * @see [createRefreshToken]
     */
    fun createTokens(userUUID: String, username: String): Tokens {
        return Tokens(createAccessToken(userUUID, username), createRefreshToken(userUUID, username))
    }

    /**
     * **AccessToken**을 생성.
     *
     * issuer는 "https://api.karanda.kr/authentication"로 설정. 토큰 유효 기간은 UTC를 사용.
     *
     * @param [userUUID]
     * @param [username]
     * @param [expire] 기본값은 [tokenProperties]에서 가져옴.
     * @return [String] AccessToken
     */
    fun createAccessToken(
        userUUID: String,
        username: String,
        expire: Long = tokenProperties.expire.toLong()
    ): String {
        val claims = mutableMapOf("username" to username)
        val tokenExpire = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(expire)

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .claims(claims)
            .signWith(buildSecretKey(tokenProperties.secretKey))
            .expiration(Date.from(tokenExpire.toInstant()))
            .compact()
    }

    /**
     * 테스트에서만 사용해야함.
     *
     * @return [String] QualificationToken
     */
    fun createQualificationToken(): String {
        val claims = mutableMapOf("platform" to "WINDOWS")
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10)

        return Jwts.builder()
            .issuer("https://api.karanda.kr/client")
            .claims(claims)
            .signWith(buildSecretKey(tokenProperties.platformKey))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    /**
     * **RefreshToken**을 생성.
     *
     * issuer는 "https://api.karanda.kr/authentication"로 설정. 토큰 유효 기간은 **UTC**를 사용.
     *
     * @param [userUUID]
     * @param [username] 사용되지 않음
     * @return [String] RefreshToken
     */
    fun createRefreshToken(userUUID: String, username: String): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(tokenProperties.refreshExpire.toLong())

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .signWith(buildSecretKey(tokenProperties.refreshKey))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    /**
     * **AccessToken** 검증 후 Spring Security 인증 객체 생성.
     *
     * 반드시 [UsernamePasswordAuthenticationToken.authenticated] 팩토리를 사용해야 한다.
     * `SecurityConfiguration`의 `authorizeHttpRequests { authorize(anyRequest, authenticated) }`가
     * `Authentication.isAuthenticated()`를 확인하는데, 2-arg 생성자는 내부에서
     * `setAuthenticated(false)`를 호출하므로 토큰이 유효해도 접근이 거부된다. 이때 인증 객체가
     * 익명이 아니라서 `ExceptionTranslationFilter`가 entry point(401)가 아니라 AccessDeniedHandler로
     * 보내 **403**이 나간다. (문서화된 계약은 유효 토큰 → 200, 무효 토큰 → 401)
     *
     * 이 프로젝트는 권한(authority) 기반 인가 규칙을 쓰지 않으므로 authorities는 비워 둔다.
     */
    fun validateAccessToken(token: String): Authentication {
        val payload = validateToken(token, buildSecretKey(tokenProperties.secretKey))
        val tokenClaims = TokenClaims(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken.authenticated(tokenClaims, token, emptyList())
    }

    /**
     * **RefreshToken** 검증 후 Spring Security 인증 객체 생성.
     *
     * `authenticated` 팩토리를 써야 하는 이유는 [validateAccessToken] 참조.
     */
    fun validateRefreshToken(token: String): Authentication {
        val payload = validateToken(token, buildSecretKey(tokenProperties.refreshKey))
        val tokenClaims = TokenClaims(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken.authenticated(tokenClaims, token, emptyList())
    }

    fun validateQualificationToken(token: String): Boolean {
        try {
            validateToken(token, buildSecretKey(tokenProperties.platformKey))
            return true
        } catch (e: ExpiredJwtException) {
            logger.warn("Expired qualification token\ntoken: $token\n${e.message}")
            return false
        } catch (e: Exception) {
            logger.error(e.message)
            return false
        }
    }

    private fun validateToken(jwt: String, key: SecretKeySpec): Claims {
        val token = jwt.removePrefix("Bearer").trim()
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    }

    private fun buildSecretKey(key: String): SecretKeySpec {
        return SecretKeySpec(key.toByteArray(), tokenProperties.algorithm)
    }
}