package kr.karanda.karandaserver.util

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.MalformedKeyException
import io.jsonwebtoken.security.SecurityException
import kr.karanda.karandaserver.data.TokenProperties
import kr.karanda.karandaserver.data.Tokens
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.service.FireStoreService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import javax.crypto.spec.SecretKeySpec

@Component
class TokenFactory(fireStoreService: FireStoreService) {

    private val tokenProperties: TokenProperties = fireStoreService.getTokenProperties()

    fun createTokens(userUUID: String, username: String): Tokens {
        return Tokens(createAccessToken(userUUID, username), createRefreshToken(username, username))
    }

    private fun createAccessToken(userUUID: String, username: String): String {
        val claims = mutableMapOf("username" to username)
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(tokenProperties.expire.toLong())

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .claims(claims)
            .signWith(SecretKeySpec(tokenProperties.secretKey.toByteArray(), tokenProperties.algorithm))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    fun createQualificationToken(): String {
        val claims = mutableMapOf("platform" to "WINDOWS")
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10)

        return Jwts.builder()
            .issuer("https://api.karanda.kr/client")
            .claims(claims)
            .signWith(SecretKeySpec(tokenProperties.platformKey.toByteArray(), tokenProperties.algorithm))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    private fun createRefreshToken(userUUID: String, username: String): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(tokenProperties.refreshExpire.toLong())

        return Jwts.builder()
            .subject(userUUID)
            .issuer("https://api.karanda.kr/authentication")
            .signWith(SecretKeySpec(tokenProperties.refreshKey.toByteArray(), tokenProperties.algorithm))
            .expiration(Date.from(now.toInstant()))
            .compact()
    }

    // This function is for access token
    fun getAuthentication(token: String): Authentication {
        val key = SecretKeySpec(tokenProperties.secretKey.toByteArray(), tokenProperties.algorithm)
        val payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        val user = User(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken(user, token)
    }

    fun getAuthenticationFromRefreshToken(token: String): Authentication {
        val key = SecretKeySpec(tokenProperties.refreshKey.toByteArray(), tokenProperties.algorithm)
        val payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        val user = User(userUUID = payload.subject, username = payload["username"].toString())
        return UsernamePasswordAuthenticationToken(user, token)
    }

    fun validateAccessToken(token: String): Boolean {
        return validateToken(token, SecretKeySpec(tokenProperties.secretKey.toByteArray(), tokenProperties.algorithm))
    }

    fun validateRefreshToken(token: String): Boolean {
        return validateToken(token, SecretKeySpec(tokenProperties.refreshKey.toByteArray(), tokenProperties.algorithm))
    }

    fun validateQualificationToken(token: String): Boolean {
        return validateToken(token, SecretKeySpec(tokenProperties.platformKey.toByteArray(), tokenProperties.algorithm))
    }

    private fun validateToken(token: String, key: SecretKeySpec): Boolean {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            return true
        } catch (e: SecurityException) {
            println("Invalid token\n${e.message}")
        } catch (e: MalformedKeyException) {
            println(
                "Invalid token\n" +
                        "${e.message}"
            )
        } catch (e: ExpiredJwtException) {
            println(
                "Expired token\n" +
                        "${e.message}"
            )
        } catch (e: UnsupportedJwtException) {
            println(
                "Unsupported Jwt token\n" +
                        "${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            println(
                "Invalid token\n" +
                        "${e.message}"
            )
        }
        return false
    }
}