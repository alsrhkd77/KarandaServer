package kr.karanda.karandaserver.repository

import kr.karanda.karandaserver.entity.BDOFamily
import kr.karanda.karandaserver.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BDOFamilyRepository: JpaRepository<BDOFamily, Long> {
    @Query("select family from Users u left outer join BDOFamily family on u.id = family.owner_id where u.user_uuid =:user_uuid and family.code =:code and family.region =:region", nativeQuery = true)
    fun findWithUserUUIDAndCodeAndRegion(@Param("user_uuid") uuid:String, @Param("code") code:String, @Param("region") region:String): BDOFamily?

    fun findByCodeAndRegionAndOwnerNot(code:String, region:String, owner:User):BDOFamily?
}