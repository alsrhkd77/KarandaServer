package kr.karanda.karandaserver.repository

import kr.karanda.karandaserver.entity.BDOFamily
import kr.karanda.karandaserver.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BDOFamilyRepository: JpaRepository<BDOFamily, Long> {
    fun findByCodeAndRegionAndOwnerNot(code:String, region:String, owner:User):BDOFamily?
}