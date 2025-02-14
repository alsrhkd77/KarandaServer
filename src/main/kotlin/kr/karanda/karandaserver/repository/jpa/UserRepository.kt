package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByUserUUID(uuid: String): User?
    fun findByDiscordId(discordId: String): User?
}