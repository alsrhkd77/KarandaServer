package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.RecruitmentPost
import org.springframework.data.jpa.repository.JpaRepository

interface RecruitmentPostRepository: JpaRepository<RecruitmentPost, Long> {
    fun findTop100ByRegion(region:String): List<RecruitmentPost>
    fun findByIdAndAuthor_UserUUID(id: Long, uuid: String): RecruitmentPost?
}