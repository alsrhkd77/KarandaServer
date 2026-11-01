package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.UserFcmSettingsDTO
import kr.karanda.karandaserver.entity.UserFcmSettings
import kr.karanda.karandaserver.enums.BdoNewsCategory
import kr.karanda.karandaserver.enums.ReportTarget
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

    /**
     * 뉴스 알림 설정 갱신 (Web 전용).
     *
     * 기존 [saveUserFcmSettings]는 뉴스 설정 필드를 갱신하지 않으므로
     * (구버전 클라이언트의 save 호출이 뉴스 설정을 초기화하지 않도록) 별도 메서드로 분리한다.
     */
    fun updateNewsSettings(
        token: String,
        uuid: String,
        newsNotify: Boolean,
        newsCategories: List<BdoNewsCategory>,
    ): UserFcmSettingsDTO {
        val value = userFcmSettingsRepository.findByTokenAndOwner_UserUUID(token, uuid)
            ?: throw InvalidArgumentException()
        value.newsNotify = newsNotify
        value.newsCategories = newsCategories.joinToString(",") { it.name }
        value.lastUpdated = ZonedDateTime.now(ZoneId.of("UTC"))
        return userFcmSettingsRepository.save(value).toDTO()
    }

    /**
     * Reporter Hub 알림 설정 갱신 (Web·Android).
     *
     * 뉴스 설정과 같은 이유로 [saveUserFcmSettings]와 분리한다(구버전 클라이언트의 save가 덮어쓰지 않도록).
     * 대상 코드는 [ReportTarget] 이름 또는 `MAINTENANCE`만 받는다.
     */
    fun updateReportHubSettings(
        token: String,
        uuid: String,
        notify: Boolean,
        targets: List<String>,
    ): UserFcmSettingsDTO {
        val allowed = ReportTarget.entries.map { it.name }.toSet() + REPORT_HUB_MAINTENANCE
        if (targets.any { it !in allowed }) throw InvalidArgumentException()
        val value = userFcmSettingsRepository.findByTokenAndOwner_UserUUID(token, uuid)
            ?: throw InvalidArgumentException()
        value.reportHubNotify = notify
        value.reportHubTargets = targets.distinct().joinToString(",")
        value.lastUpdated = ZonedDateTime.now(ZoneId.of("UTC"))
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

    companion object {
        /** Reporter Hub 서버 열림(점검 종료) 알림 대상 코드 */
        const val REPORT_HUB_MAINTENANCE = "MAINTENANCE"
    }
}
