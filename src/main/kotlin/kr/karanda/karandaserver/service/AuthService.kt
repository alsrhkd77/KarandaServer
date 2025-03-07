package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.api.DiscordApi
import kr.karanda.karandaserver.dto.DiscordUserData
import kr.karanda.karandaserver.dto.Tokens
import kr.karanda.karandaserver.dto.UserDTO
import kr.karanda.karandaserver.entity.User
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.TokenUtils
import kr.karanda.karandaserver.util.UUIDUtils
import org.springframework.stereotype.Service

/**
 * Auth 관련 로직
 *
 * 유저 생성, 삭제, 로그인 시 유저 정보 갱신, 토큰 생성
 *
 * @property [userRepository]
 * @property [discordApi]
 * @property [tokenUtils]
 *
 * @see [UserRepository]
 * @see [DiscordApi]
 * @see [TokenUtils]
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val discordApi: DiscordApi,
    val tokenUtils: TokenUtils
) {
    /**
     * 토큰 없이 로그인 시 호출되는 로그인
     *
     * [code]와 [redirectURL]을 이용해 **Discord Api**에서 데이터를 가져와 **Karanda**에 없는 유저일 경우
     * 새로운 유저 생성함. 유저 정보를 이용해 [Tokens]를 만들어 반환.
     *
     * @param [code] 사용자가 **Discord** 로그인을 통해 얻을 수 있음.
     * @param [redirectURL] **Discord 개발자 센터**에 등록된 URL.
     *
     * @return [Tokens] 유저 정보를 이용해 [Tokens.accessToken]과 [Tokens.refreshToken]을 생성해 반환.
     *
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] **Discord Api**에서
     * 데이터를 가져오지 못했을 경우 throw 될 수 있음.
     *
     * @see [createUser]
     */
    fun authenticate(code: String, redirectURL: String): Tokens {
        val discordToken = discordApi.exchangeCode(code = code, redirectURL = redirectURL)
        val discordUser = discordApi.getUserDataByToken(discordToken)
        val user = userRepository.findByDiscordId(discordUser.id) ?: createUser(discordUser)
        return tokenUtils.createTokens(userUUID = user.userUUID, username = user.userName)
    }

    /**
     * 신규 유저 생성. [authenticate]에서만 호출 할 수 있음.
     *
     * @param [discordUserData] **Discord Api**를 사용해 가져온 유저 데이터.
     *
     * @return [User]
     *
     * @see [authenticate]
     */
    private fun createUser(discordUserData: DiscordUserData): User {
        val user = User(
            userUUID = UUIDUtils().generateUUID1().toString(),
            discordId = discordUserData.id,
            userName = discordUserData.username,
            discriminator = discordUserData.discriminator,
            avatarHash = discordUserData.avatar
        )
        return userRepository.saveAndFlush(user)
    }

    /**
     * 토큰을 사용한 로그인 시 호출되는 로그인.
     *
     * @param [userUUID] 토큰에서 추출한 uuid.
     *
     * @return [UserDTO] DTO로 변환 후 반환.
     *
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] **Discord Api**에서
     * 데이터를 가져오지 못했을 경우 throw 될 수 있음.
     * @throws [UnknownUserException] 제공된 uuid와 일치하는 유저가 없을 경우 throw.
     */
    fun authorization(userUUID: String): UserDTO {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        discordApi.getUserDataById(user.discordId).let {
            user.discordId = it.id
            user.userName = it.username
            user.avatarHash = it.avatar
        }
        return userRepository.saveAndFlush(user).toUserDTO()
    }

    /**
     * 토큰 생성. 토큰 Refresh 시 호출.
     *
     * @throws [UnknownUserException] 제공된 uuid와 일치하는 유저가 없을 경우 throw.
     */
    fun createTokens(userUUID: String): Tokens {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        return tokenUtils.createTokens(userUUID = user.userUUID, username = user.userName)
    }

    /**
     * 회원 탈퇴(=유저 삭제)
     *
     * @throws [UnknownUserException] 제공된 uuid와 일치하는 유저가 없을 경우 throw.
     */
    fun unregister(userUUID: String) {
        val user = userRepository.findByUserUUID(userUUID) ?: throw UnknownUserException()
        userRepository.delete(user)
    }
}