package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BDOFamily
import org.springframework.data.jpa.repository.JpaRepository

interface BDOFamilyRepository : JpaRepository<BDOFamily, Long> {
    fun findByCodeAndRegionAndOwner_UserUUID(code: String, region: String, userUUID: String): BDOFamily?
    fun findByCodeAndRegionAndVerifiedAndOwner_UserUUID(code: String, region: String, verified: Boolean, userUUID: String): BDOFamily?
    fun findAllByCodeAndRegionAndOwner_UserUUIDNot(code: String, region: String, ownerUserUUID: String): MutableList<BDOFamily>
    fun findAllByOwner_UserUUID(userUUID: String): List<BDOFamily>
}