package kr.karanda.karandaserver.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import kr.karanda.karandaserver.dto.Applicant
import kr.karanda.karandaserver.dto.Applicant as ApplicantDTO
import java.time.ZonedDateTime

@Entity(name = "applicant")
class Applicant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var code: String? = null,
    var appliedAt: ZonedDateTime,
    var canceledAt: ZonedDateTime? = null,
    var rejectedAt: ZonedDateTime? = null,
    var approvedAt: ZonedDateTime? = null,
    var reason: String? = null,
    @ManyToOne
    var post: RecruitmentPost,
    @ManyToOne
    var owner: User
) {
    fun toDTO(): Applicant {
        return ApplicantDTO(
            code = code,
            appliedAt = appliedAt,
            canceledAt = canceledAt,
            rejectedAt = rejectedAt,
            approvedAt = approvedAt,
            reason = reason,
            user = owner.toUserDTO(),
            postId = post.id!!,
        )
    }
}