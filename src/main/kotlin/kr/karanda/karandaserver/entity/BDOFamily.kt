package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import java.time.ZonedDateTime
import kr.karanda.karandaserver.dto.BDOFamily as DTO

@Entity(name = "bdo_family")
class BDOFamily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var familyName: String,
    var mainClass: String,
    var region: String,
    var code: String,
    var verified: Boolean = false,
    var lifeSkillIsPrivate: Boolean,
    var startVerification: ZonedDateTime? = null,
    var firstVerification: ZonedDateTime? = null,
    var secondVerification: ZonedDateTime? = null,
    var lastUpdated: ZonedDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "owner_id")
    var owner: User
) {
    fun toDTO(): DTO{
        return DTO(
            familyName = familyName,
            mainClass = mainClass,
            region = region,
            code = code,
            verified = verified,
            lifeSkillIsPrivate = lifeSkillIsPrivate,
            startVerification = startVerification,
            firstVerification = firstVerification,
            secondVerification = secondVerification,
            lastUpdated = lastUpdated,
        )
    }
}