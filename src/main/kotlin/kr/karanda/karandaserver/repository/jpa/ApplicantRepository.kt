package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.Applicant
import org.springframework.data.jpa.repository.JpaRepository

interface ApplicantRepository : JpaRepository<Applicant, Long> {
    fun existsByCodeAndPost_Id(code: String, postId: Long): Boolean
    fun findByPost_IdAndOwner_Id(postId: Long, ownerId: Long): Applicant?
    fun findByPost_IdAndOwner_DiscordIdAndPost_Author_Id(
        postId: Long,
        ownerDiscordId: String,
        postAuthorId: Long
    ): Applicant?
    fun findTop100ByOwner_UserUUID(uuid: String): MutableList<Applicant>
}