package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.BDOFamilyDTO
import kr.karanda.karandaserver.enums.BDORegion

@Entity(name = "bdo_family")
class BDOFamily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var code: String,
    var region: String,
    var familyName: String,
    var mainClass: String,
    var verified: Boolean = false,

    @ManyToOne
    var owner: User,

    @OneToMany(mappedBy = "family", orphanRemoval = true)
    val verificationLog: List<BDOFamilyVerification> = mutableListOf(),
) {
    fun toDTO() = BDOFamilyDTO(
        code = code,
        region = BDORegion.valueOf(region),
        familyName = familyName,
        mainClass = mainClass,
        verified = verified,
    )
}