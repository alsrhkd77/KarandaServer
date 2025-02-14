package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.User as UserDTO
import kr.karanda.karandaserver.dto.TokenClaims as TokenClaims

@Entity(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_uuid", unique = true)
    var userUUID: String,
    @Column(unique = true)
    var discordId: String,
    var userName: String,
    var avatarHash: String?,

    @OneToMany(mappedBy = "author")
    val recruitmentPost: List<RecruitmentPost> = mutableListOf(),

    @OneToMany(mappedBy = "owner")
    val applied: List<Applicant> = mutableListOf(),
) {
    fun toTokenClaims() = TokenClaims(userUUID = userUUID, username = userName)
    fun toUserDTO(): UserDTO {
        return UserDTO(
            discordId = discordId,
            avatar = avatarHash?.apply { "${discordId}/${this}.png" },
            username = userName,
        )
    }
}