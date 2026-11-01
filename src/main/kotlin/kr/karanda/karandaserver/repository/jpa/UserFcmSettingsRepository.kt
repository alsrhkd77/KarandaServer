package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.UserFcmSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface UserFcmSettingsRepository: JpaRepository<UserFcmSettings, Long> {
    fun findByTokenAndOwner_UserUUID(token: String, uuid: String): UserFcmSettings?
    fun findAllByOwner_UserUUID(uuid: String): List<UserFcmSettings>
    fun findAllByRegionAndPartyFinderIsTrue(region: String): List<UserFcmSettings>
    fun findAllByNewsNotifyIsTrue(): List<UserFcmSettings>

    @Transactional
    @Modifying
    @Query("delete from user_fcm_settings s where s.token in :tokens")
    fun deleteAllByTokenIn(@Param("tokens") tokens: Collection<String>): Int
}
