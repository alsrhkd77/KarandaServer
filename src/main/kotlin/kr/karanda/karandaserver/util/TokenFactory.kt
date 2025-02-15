package kr.karanda.karandaserver.util

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.MalformedKeyException
import io.jsonwebtoken.security.SecurityException
import kr.karanda.karandaserver.data.TokenProperties
import kr.karanda.karandaserver.data.Tokens
import kr.karanda.karandaserver.dto.TokenClaims
import kr.karanda.karandaserver.repository.DefaultDataRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
class TokenFactory(private val defaultDataRepository: DefaultDataRepository) {

    val tokenProperties: TokenProperties
        get() = defaultDataRepository.getTokenProperties()

    fun createTokens(userUUID: String, username: String): Tokens {
        return Tokens(createAccessToken(userUUID, username), createRefreshToken(userUUID, username))
    }

    private fun createAccessToken(userUUID: String, username: String): String {
        val claims = mutableMapOf("username" to username)
        val expire = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(tokenProperties.expire.toLong())
        //val expire = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(10)

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .claims(claims)
            .signWith(secretKey(tokenProperties.secretKey))
            .expiration(Date.from(expire.toInstant()))
            .compact()
    }

    fun createQualificationToken(): String {
        val claims = mutableMapOf("platform" to "WINDOWS")
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10)

        return Jwts.builder()
            .issuer("https://api.karanda.kr/client")
            .claims(claims)
            .signWith(secretKey(tokenProperties.platformKey))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    private fun createRefreshToken(userUUID: String, username: String): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(tokenProperties.refreshExpire.toLong())

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .signWith(secretKey(tokenProperties.refreshKey))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    // This function is for access token
    fun getAuthentication(token: String): Authentication {
        val payload = Jwts
            .parser()
            .verifyWith(secretKey(tokenProperties.secretKey))
            .build()
            .parseSignedClaims(token)
            .payload
        val tokenClaims = TokenClaims(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken(tokenClaims, token)
    }

    fun getAuthenticationFromRefreshToken(token: String): Authentication {
        val payload = Jwts
            .parser()
            .verifyWith(secretKey(tokenProperties.refreshKey))
            .build()
            .parseSignedClaims(token)
            .payload
        val tokenClaims = TokenClaims(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken(tokenClaims, token)
    }

    fun validateAccessToken(token: String): Boolean {
        return try {
            validateToken(token, secretKey(tokenProperties.secretKey))
        } catch (e: Exception) {
            false
        }
    }

    fun validateRefreshToken(token: String): Boolean {
        try {
            return validateToken(token, secretKey(tokenProperties.refreshKey))
        } catch (e: ExpiredJwtException) {
            println("Expired refresh token\n${e.message}")
        }
        return false
    }

    fun validateQualificationToken(token: String): Boolean {
        try {
            return validateToken(token, secretKey(tokenProperties.platformKey))
        } catch (e: ExpiredJwtException) {
            println("Expired Qualification token\n${e.message}")
        }
        return false
    }

    private fun validateToken(token: String, key: SecretKeySpec): Boolean {
        token.removePrefix("Bearer ")
        var result = false
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            result = true
        } catch (e: SecurityException) {
            println("Invalid token\n${e.message}")
        } catch (e: MalformedKeyException) {
            println("Invalid token\n${e.message}")
        } catch (e: ExpiredJwtException) {
            throw e
        } catch (e: UnsupportedJwtException) {
            println("Unsupported Jwt token\n${e.message}")
        } catch (e: IllegalArgumentException) {
            println("Invalid token\n${e.message}")
        } catch (e: Exception) {
            println("Unsupported exception from {validateToken}\n${e.message}")
        }
        return result
    }

    fun secretKey(key: String): SecretKeySpec {
        return SecretKeySpec(key.toByteArray(), tokenProperties.algorithm)
    }
}