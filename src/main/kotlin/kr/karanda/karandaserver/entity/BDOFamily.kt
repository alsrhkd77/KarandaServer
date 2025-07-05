package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.BDOFamilyDTO
import kr.karanda.karandaserver.enums.BDORegion
import java.time.ZonedDateTime

@Entity(name = "bdo_family")
class BDOFamily(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var code: String,
    var region: String,
    var familyName: String,
    var mainClass: String,
    var guild: String? = null,      //"": 길드 없음, null: 비공개
    var maxGearScore: Int? = null,
    var verified: Boolean = false,
    var lastUpdated:ZonedDateTime? = null,

    @OneToOne
    var owner: User,

    @OneToMany(mappedBy = "family", orphanRemoval = true)
    val verificationLog: List<BDOFamilyVerification> = mutableListOf(),
) {
    fun toDTO() = BDOFamilyDTO(
        code = code,
        region = BDORegion.valueOf(region),
        familyName = familyName,
        mainClass = mainClass,
        maxGearScore = maxGearScore,
        verified = verified,
        lastUpdated = lastUpdated,
    )
}