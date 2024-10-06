package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.User as UserDTO

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
) {
    fun toDTO() = UserDTO(userUUID = userUUID, username = userName)
}