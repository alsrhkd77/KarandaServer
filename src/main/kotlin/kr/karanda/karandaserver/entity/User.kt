package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.UserDTO as UserDTO
import kr.karanda.karandaserver.dto.TokenClaims as TokenClaims

//https://discord.com/developers/docs/resources/user#get-current-user
@Entity(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_uuid", unique = true)
    var userUUID: String,
    @Column(unique = true)
    var discordId: String,
    var discordDiscriminator: String,
    var userName: String,
    var avatarHash: String?,

    @OneToMany(mappedBy = "author")
    val recruitmentPost: List<RecruitmentPost> = mutableListOf(),

    @OneToMany(mappedBy = "owner")
    val applied: List<Applicant> = mutableListOf(),

    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    val families: List<BDOFamily> = mutableListOf(),

    @OneToOne(fetch = FetchType.EAGER)
    var mainFamily: BDOFamily? = null,
) {
    fun toUserDTO(): UserDTO {
        val embedded = discordDiscriminator.toIntOrNull()?.let { it % 6 } ?: 0
        return UserDTO(
            discordId = discordId,
            username = userName,
            avatar = avatarHash?.apply { "https://cdn.discordapp.com/avatars/${discordId}/${this}.png" }
                ?: "https://cdn.discordapp.com/embed/avatars/${embedded}.png",
            mainFamily = mainFamily?.toDTO(),
        )
    }
}