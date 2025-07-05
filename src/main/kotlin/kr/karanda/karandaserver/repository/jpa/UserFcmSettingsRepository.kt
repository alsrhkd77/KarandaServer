package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.UserFcmSettings
import org.springframework.data.jpa.repository.JpaRepository

interface UserFcmSettingsRepository: JpaRepository<UserFcmSettings, Long> {
    fun findByTokenAndOwner_UserUUID(token: String, uuid: String): UserFcmSettings?
    fun findAllByOwner_UserUUID(uuid: String): List<UserFcmSettings>
    fun findAllByRegionAndAdventurerHubIsTrue(region: String): List<UserFcmSettings>
}