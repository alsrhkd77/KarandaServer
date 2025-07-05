package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.UserFcmSettingsDTO
import kr.karanda.karandaserver.entity.UserFcmSettings
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.repository.jpa.UserFcmSettingsRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class FcmService(
    private val userFcmSettingsRepository: UserFcmSettingsRepository,
    private val userRepository: UserRepository,
) {
    fun getUserFcmSettings(token: String, uuid: String): UserFcmSettingsDTO? {
        return userFcmSettingsRepository.findByTokenAndOwner_UserUUID(token, uuid)?.toDTO()
    }

    fun saveUserFcmSettings(userFcmSettings: UserFcmSettingsDTO, uuid: String): UserFcmSettingsDTO {
        val value = userFcmSettingsRepository.findByTokenAndOwner_UserUUID(userFcmSettings.token, uuid)?.apply {
            region = userFcmSettings.region.name
            partyFinder = userFcmSettings.partyFinder
            fieldBoss = userFcmSettings.fieldBoss
            lastUpdated = ZonedDateTime.now(ZoneId.of("UTC"))
        } ?: UserFcmSettings(
            token = userFcmSettings.token,
            region = userFcmSettings.region.name,
            partyFinder = userFcmSettings.partyFinder,
            fieldBoss = userFcmSettings.fieldBoss,
            lastUpdated = ZonedDateTime.now(ZoneId.of("UTC")),
            owner = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        )
        return userFcmSettingsRepository.save(value).toDTO()
    }

    fun unregisterFcmToken(token: String, uuid: String) {
        val value =
            userFcmSettingsRepository.findByTokenAndOwner_UserUUID(token, uuid) ?: throw InvalidArgumentException()
        userFcmSettingsRepository.delete(value)
    }

    fun updateFcmToken(oldToken: String, newToken: String, uuid: String) {
        val value = userFcmSettingsRepository.findByTokenAndOwner_UserUUID(oldToken, uuid)?.apply { token = newToken }
            ?: throw InvalidArgumentException()
        userFcmSettingsRepository.saveAndFlush(value)
    }
}