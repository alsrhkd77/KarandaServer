package kr.karanda.karandaserver.service

import jakarta.transaction.Transactional
import kr.karanda.karandaserver.entity.BDOFamily as BDOFamilyEntity
import kr.karanda.karandaserver.dto.BDOFamily as BDOFamilyDTO
import kr.karanda.karandaserver.exception.UnknownUser
import kr.karanda.karandaserver.repository.BDOFamilyRepository
import kr.karanda.karandaserver.repository.UserRepository
import kr.karanda.karandaserver.util.BDOWebParser
import kr.karanda.karandaserver.util.difference
import org.apache.coyote.BadRequestException
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class BDOFamilyService(val userRepository: UserRepository, val bdoFamilyRepository: BDOFamilyRepository) {
    val webParser = BDOWebParser()

    fun startVerification(uuid: String, code: String, region: String, familyName: String): BDOFamilyDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val data = webParser.parseProfile(profileCode = code, region = region)
        var family = user.bdoFamily.firstOrNull { it.code == code && it.region == region } ?: BDOFamilyEntity(
            familyName = data.familyName,
            mainClass = data.mainClass,
            region = data.region,
            code = code,
            verified = false,
            lifeSkillIsPrivate = data.lifeSkillLevel == null,
            owner = user
        )

        if (family.verified) {
            throw Exception()
        } else if (family.id == null && data.familyName != familyName) {
            throw Exception()
        } else {
            family.startVerification = ZonedDateTime.now(ZoneId.of("UTC"))
            family.firstVerification = null
            family.secondVerification = null
            family.familyName = data.familyName
            family.mainClass = data.mainClass
            family.lifeSkillIsPrivate = data.lifeSkillLevel == null
        }

        family = bdoFamilyRepository.save(family)
        /*if (user.mainFamily == null) {
            user.mainFamily = family
            userRepository.save(user)
        }*/

        return family.toDTO()
    }

    @Transactional
    fun verifyFamily(uuid: String, code: String, region: String): BDOFamilyDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val family =
            user.bdoFamily.firstOrNull { it.code == code && it.region == region && !it.verified }
                ?: throw BadRequestException()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val data = webParser.parseProfile(profileCode = code, region = region)
        if(family.startVerification == null || family.startVerification!!.difference(now).toHours() >= 3) {
            family.startVerification = now
            family.firstVerification = null
            family.secondVerification = null
            family.familyName = data.familyName
            family.mainClass = data.mainClass
            family.lifeSkillIsPrivate = data.lifeSkillLevel == null
        } else if(family.firstVerification == null && family.lifeSkillIsPrivate != (data.lifeSkillLevel == null)){
            /* 1단계 완료 */
            family.firstVerification = now
            family.secondVerification = null
        } else if (family.firstVerification != null && family.lifeSkillIsPrivate == (data.lifeSkillLevel == null)){
            /* 2단계 완료 */
            family.secondVerification = now
            family.lastUpdated = now
            family.verified = true
            family.familyName = data.familyName

            /*
            * 동일 가문 인증 확인 및 처리
            *
            * 다른 유저가 가진 동일 가문에 대한 인증은 해제,
            * 대표 가문의 경우 그것도 해제
            * */
            bdoFamilyRepository.findByCodeAndRegionAndOwnerNot(code = code, region = region, owner = user)?.let {
                val owner = it.owner
                if (owner.mainFamily?.id == it.id) {
                    owner.mainFamily = null
                    //userRepository.save(owner)
                }
                it.verified = false
                //it.startVerification = null
                //it.firstVerification = null
                //it.secondVerification = null
                //bdoFamilyRepository.save(it)
            }

            if (user.mainFamily == null) {
                user.mainFamily = family
                //userRepository.save(user)
            }
        } else {
            throw BadRequestException()
        }
        return family.toDTO()
    }

    fun getAllFamily(uuid: String): List<BDOFamilyDTO> {
        return userRepository.findByUserUUID(uuid)?.bdoFamily?.map { it.toDTO() } ?: throw UnknownUser()
    }

    fun changeMainFamily(uuid: String, code: String, region: String): BDOFamilyDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val family = user.bdoFamily.firstOrNull { it.code == code && it.region == region && it.verified } ?: throw UnknownUser()
        user.mainFamily = family
        userRepository.save(user)
        return family.toDTO()
    }

    fun deleteFamily(uuid: String, code: String, region: String) {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val family: BDOFamilyEntity? = user.bdoFamily.firstOrNull { it.code == code && it.region == region }
        if (family != null) {
            if (user.mainFamily?.id == family.id) {
                user.mainFamily = null
                userRepository.save(user)
            }
            bdoFamilyRepository.delete(family)
        }
    }

    @Transactional
    fun refreshFamilyData(uuid: String, code: String, region: String): BDOFamilyDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val family: BDOFamilyEntity =
            user.bdoFamily.firstOrNull { it.code == code && it.region == region } ?: throw UnknownUser()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val data = webParser.parseProfile(profileCode = code, region = region)
        family.familyName = data.familyName
        family.mainClass = data.mainClass
        family.lastUpdated = now
        //bdoFamilyRepository.save(family)
        return family.toDTO()
    }
}