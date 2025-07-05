package kr.karanda.karandaserver.service

import jakarta.transaction.Transactional
import kr.karanda.karandaserver.dto.BDOFamilyDTO
import kr.karanda.karandaserver.dto.BDOFamilyVerificationDTO
import kr.karanda.karandaserver.dto.UserDTO
import kr.karanda.karandaserver.entity.BDOFamily
import kr.karanda.karandaserver.entity.BDOFamilyVerification
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.properties.AdventurerProfileUrlBase
import kr.karanda.karandaserver.repository.jpa.BDOFamilyRepository
import kr.karanda.karandaserver.repository.jpa.BDOFamilyVerificationRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.WebUtils
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 검은사막 가문 인증 관련 로직.
 *
 * 가문 등록, 소유 확인, 가문 정보 업데이트, 대표 가문 설정, 삭제.
 *
 * 가문 확인은 프로세스 시작 후 10분 이내에 완료되어야 함. 10분 이내에 완료하지 못 할 경우 다시 시작 필요.
 *
 * @property [userRepository]
 * @property [familyRepository]
 * @property [familyVerificationRepository]
 * @property [adventurerProfileUrlBase]
 */
@Service
class BDOFamilyService(
    private val userRepository: UserRepository,
    private val familyRepository: BDOFamilyRepository,
    private val familyVerificationRepository: BDOFamilyVerificationRepository,
    private val adventurerProfileUrlBase: AdventurerProfileUrlBase
) {
    val utils get() = WebUtils()

    /**
     * **Karanda** 계정에 신규 가문 등록.
     *
     * 계정에 [code]와 [region]이 일치하는 가문이 이미 등록되어 있을 경우 기존 가문 반환.
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @return [BDOFamilyDTO] 가문 정보
     *
     * @throws [UnknownUserException]
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] 가문 정보 가져오기에 실패할 경우 throw.
     */
    fun registerNewFamily(uuid: String, code: String, region: BDORegion): BDOFamilyDTO {
        val url = "${adventurerProfileUrlBase.getUrl(region)}?profileTarget=${code}"
        return familyRepository.findByCodeAndRegionAndOwner_UserUUID(
            code = code,
            region = region.name,
            userUUID = uuid
        )?.toDTO() ?: utils.getAdventurerProfile(url).let {
            familyRepository.save(
                BDOFamily(
                    code = code,
                    region = region.name,
                    familyName = it.familyName,
                    mainClass = it.mainClass,
                    //guild = it.guild,
                    //maxGearScore = it.maxGearScore,
                    owner = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
                )
            ).toDTO()
        }
    }

    /**
     * 가문 확인 프로세스 시작
     *
     * 10분 이내에 시작한 프로세스가 있다면 해당 프로세스 스타트 포인트 반환.
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @return [BDOFamilyVerificationDTO]
     *
     * @throws [UnknownUserException] 인증 해야할 가문이 존재하지 않을 경우 throw.
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] 가문 정보 가져오기에 실패할 경우 throw.
     */
    fun startVerification(uuid: String, code: String, region: BDORegion): BDOFamilyVerificationDTO {
        val url = "${adventurerProfileUrlBase.getUrl(region)}?profileTarget=${code}"
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val family = familyRepository.findByCodeAndRegionAndVerifiedAndOwner_UserUUID(
            code = code,
            region = region.name,
            verified = false,
            userUUID = uuid
        ) ?: throw UnknownUserException()
        val verification = familyVerificationRepository.findByFamilyAndStartPointAndCreatedAtAfterOrderByCreatedAtDesc(
            family = family,
            startPoint = true,
            createdAtAfter = now.minusMinutes(10)
        )
        return verification?.toDTO() ?: utils.getAdventurerProfile(url).let {
            familyVerificationRepository.save(
                BDOFamilyVerification(
                    family = family,
                    createdAt = now,
                    startPoint = true,
                    lifeSkillIsLocked = it.lifeSkillIsLocked,
                    contributionPointIsLocked = it.contributionPoints == null
                )
            ).toDTO()
        }
    }

    /**
     * 가문 확인 시도.
     *
     * 10분 이내에 시작된 가문 인증 프로세스 스타트 포인트와 비교. **생활 스킬 레벨** 숨김 여부로 판단.
     * 사칭 및 중복 방지를 위해 인증 성공 시 다른 계정들의 가문 중 [code]와 [region]이 일치하는 가문 모두 인증 취소.
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @return [BDOFamilyDTO]
     *
     * @throws [UnknownUserException] 해당 계정에 등록된 일치하는 가문이 없을 경우.
     * @throws [InvalidArgumentException] 10분 이내에 시작된 가문 인증 프로세스 스타트 포인트가 존재하지 않을 경우.
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] 가문 정보 가져오기에 실패할 경우 throw.
     */
    @Transactional
    fun verifyFamily(uuid: String, code: String, region: BDORegion): BDOFamilyDTO {
        val url = "${adventurerProfileUrlBase.getUrl(region)}?profileTarget=${code}"
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val family = familyRepository.findByCodeAndRegionAndVerifiedAndOwner_UserUUID(
            code = code,
            region = region.name,
            verified = false,
            userUUID = uuid
        ) ?: throw UnknownUserException()
        val startPoint = familyVerificationRepository.findByFamilyAndStartPointAndCreatedAtAfterOrderByCreatedAtDesc(
            family = family,
            startPoint = true,
            createdAtAfter = now.minusMinutes(10)
        ) ?: throw InvalidArgumentException()
        val profile = utils.getAdventurerProfile(url)
        val verifications = familyVerificationRepository.findAllByFamilyAndStartPointAndCreatedAtAfterOrderByCreatedAtDesc(
            family = family,
            startPoint = false,
            createdAtAfter = startPoint.createdAt
        ).map { it.toDTO() }
        familyVerificationRepository.save(
            BDOFamilyVerification(
                family = family,
                createdAt = now,
                startPoint = false,
                lifeSkillIsLocked = profile.lifeSkillIsLocked,
                contributionPointIsLocked = profile.contributionPoints == null
            )
        )
        if(verifications.any { it.lifeSkillIsLocked != startPoint.lifeSkillIsLocked}) {
            //Step 2
            if(startPoint.lifeSkillIsLocked == profile.lifeSkillIsLocked){
                familyRepository.findAllByCodeAndRegionAndOwner_UserUUIDNot(
                    code = code,
                    region = region.name,
                    ownerUserUUID = uuid
                ).forEach { it.verified = false }
                family.apply {
                    familyName = profile.familyName
                    mainClass = profile.mainClass
                    verified = true
                }
            } else {
                throw InvalidArgumentException()
            }
        } else {
            //Step 1
            if(startPoint.lifeSkillIsLocked != profile.lifeSkillIsLocked){
                family.apply {
                    familyName = profile.familyName
                    mainClass = profile.mainClass
                }
            } else {
                throw InvalidArgumentException()
            }
        }
        return family.toDTO()
    }

    /**
     * 계정에 등록된 가문 삭제.
     *
     * 계정에 등록된 가문 중 [code]와 [region]이 일치하는 가문을 삭제.
     * 메인 가문일 경우 메인 가문은 `null`이 됨.
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @throws [UnknownUserException] [uuid]와 일치하는 계정이 없을 경우
     */
    fun unregisterFamily(uuid: String, code: String, region: BDORegion) {
        val user = userRepository.findByUserUUID(uuid = uuid) ?: throw UnknownUserException()
        if (user.bdoFamily != null && user.bdoFamily!!.code == code && user.bdoFamily!!.region == region.name) {
            val family = user.bdoFamily
            user.bdoFamily = null
            familyRepository.delete(family!!)
        } else {
            familyRepository.findByCodeAndRegionAndOwner_UserUUID(code = code, region = region.name, userUUID = uuid)
                ?.let {
                    familyRepository.delete(it)
                }
        }
    }

    /**
     * 대표 가문 설정. (deprecated)
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @return [UserDTO] 갱신된 유저 정보 반환.
     *
     * @throws [InvalidArgumentException] 제공된 [code], [region], [uuid]가 모두 일치하는 가문이 없을 경우 throw.
     */
    fun setMainFamily(uuid: String, code: String, region: BDORegion): UserDTO {
        val user = userRepository.findByUserUUID(uuid = uuid) ?: throw UnknownUserException()
        val family = familyRepository.findByCodeAndRegionAndOwner_UserUUID(
            code = code,
            region = region.name,
            userUUID = uuid
        ) ?: throw InvalidArgumentException()
        user.bdoFamily = family
        return user.toUserDTO()
    }

    /**
     * 가문 정보 갱신.
     *
     * @param [uuid] 유저 UUID
     * @param [code] 가문 프로필 code
     * @param [region] 해당 가문 소속 서버
     *
     * @return [BDOFamilyDTO] 갱신된 가문 정보 반환.
     *
     * @throws [InvalidArgumentException] 제공된 [code], [region], [uuid]가 모두 일치하는 가문이 없을 경우 throw.
     * @throws [kr.karanda.karandaserver.exception.ExternalApiException] 가문 정보 가져오기에 실패할 경우 throw.
     */
    @Transactional
    fun updateFamilyData(uuid: String, code: String, region: BDORegion): BDOFamilyDTO {
        val url = "${adventurerProfileUrlBase.getUrl(region)}?profileTarget=${code}"
        val family = familyRepository.findByCodeAndRegionAndVerifiedAndOwner_UserUUID(
            code = code,
            region = region.name,
            verified = true,
            userUUID = uuid
        ) ?: throw InvalidArgumentException()
        utils.getAdventurerProfile(url).run {
            family.familyName = familyName
            family.mainClass = mainClass
            family.maxGearScore = maxGearScore
            family.lastUpdated = ZonedDateTime.now(ZoneId.of("UTC"))
        }
        return family.toDTO()
    }

    /**
     * 가문 목록 조회. (deprecated)
     *
     * 계정에 등록된 가문이 없거나 일치하는 계정이 없을 경우 `emptyList` 반환.
     *
     * @param [uuid] 유저 UUID
     *
     * @return List<[BDOFamilyDTO]> 가문 목록
     */
    fun getFamilies(uuid: String): List<BDOFamilyDTO> {
        return familyRepository.findAllByOwner_UserUUID(uuid).map { it.toDTO() }
    }
}