package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.BDOFamilyVerificationDTO
import java.time.ZonedDateTime

@Entity(name = "bdo_family_verification")
class BDOFamilyVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var lifeSkillIsLocked: Boolean,
    var contributionPointIsLocked: Boolean,
    var createdAt: ZonedDateTime,
    var startPoint: Boolean = false,

    @ManyToOne
    var family: BDOFamily
) {
    fun toDTO() = BDOFamilyVerificationDTO(
        lifeSkillIsLocked = lifeSkillIsLocked,
        contributionPointIsLocked = contributionPointIsLocked,
        createdAt = createdAt
    )
}