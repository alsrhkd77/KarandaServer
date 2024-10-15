package kr.karanda.karandaserver.service

import kr.karanda.karandaserver.dto.BDOFamily
import kr.karanda.karandaserver.dto.User as UserDTO
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
        if (user.mainFamily == null) {
            user.mainFamily = family
            userRepository.save(user)
        }

        return family.toDTO()
    }

    fun verifyFamily(uuid: String, code: String, region: String): BDOFamilyDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val family =
            user.bdoFamily.firstOrNull { it.code == code && it.region == region && !it.verified } ?: throw BadRequestException()
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val data = webParser.parseProfile(profileCode = code, region = region)
        if (family.startVerification != null && family.startVerification!!.difference(now).toHours() < 3) {
            if (family.firstVerification != null && family.firstVerification!!.difference(now).toHours() < 3) {
                /* 2단계 완료 */
                family.secondVerification = now
                family.verified = true
                family.familyName = data.familyName

                /* 동일 가문 인증 확인 및 처리 */
                bdoFamilyRepository.findByCodeAndRegionAndOwnerNot(code = code, region = region, owner = user)?.let {
                    it.verified = false
                    it.startVerification = null
                    it.firstVerification = null
                    it.secondVerification = null
                    bdoFamilyRepository.save(it)
                }

                if (user.mainFamily?.verified == false){
                    user.mainFamily = family
                    userRepository.save(user)
                }
            } else {
                /* 1단계 완료 */
                family.firstVerification = now
                family.secondVerification = null
            }
            bdoFamilyRepository.saveAndFlush(family)
            return family.toDTO()
        } else {
            throw Exception()
        }
    }

    fun getAllFamily(uuid: String): List<BDOFamilyDTO> {
        return userRepository.findByUserUUID(uuid)?.bdoFamily?.map { it.toDTO() } ?: throw UnknownUser()
    }

    fun changeMainFamily(uuid: String, code: String, region: String): UserDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        user.bdoFamily.firstOrNull{
            it.code == code && it.region == region
        }?.let {
            user.mainFamily = it
            userRepository.save(user)
        }
        return user.toDTO()
    }
}