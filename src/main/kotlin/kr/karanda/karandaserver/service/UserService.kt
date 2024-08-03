package kr.karanda.karandaserver.service


import kr.karanda.karandaserver.dto.User as UserDTO
import kr.karanda.karandaserver.repository.UserRepository
import kr.karanda.karandaserver.util.UUIDFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kr.karanda.karandaserver.entity.User as UserEntity

@Service
class UserService(val userRepository: UserRepository) {
    fun getByDiscordId(discordId: String): UserDTO? {
        val user = userRepository.findByDiscordId(discordId)
        return user?.toDTO()
    }

    fun getByUUID(uuid: String): UserDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User not found"
        )
        return user.toDTO()
    }

    fun getUserEntityByUUID(uuid: String): UserEntity {
        val user = userRepository.findByUserUUID(uuid) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User not found"
        )
        return user
    }

    fun getDiscordIdByUUID(uuid: String): String {
        val user = userRepository.findByUserUUID(uuid) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User not found"
        )
        return user.discordId
    }

    fun createUser(username: String, discordId: String): UserDTO {
        val uuid = UUIDFactory().generateUUID1()
        var user = UserEntity(userUUID = uuid.toString(), userName = username, discordId = discordId)
        user = userRepository.saveAndFlush(user)
        return user.toDTO()
    }

    fun updateUserFromEntity(user: UserEntity) {
        userRepository.saveAndFlush(user)
    }

    fun deleteUserByUUID(uuid: String) {
        val user = userRepository.findByUserUUID(uuid) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User not found"
        )
        userRepository.delete(user)
    }

    fun updateUsername(uuid:String, username: String): UserDTO {
        var user = userRepository.findByUserUUID(uuid) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "User not found"
        )
        if(user.userName == username){
            return user.toDTO()
        }
        user.userName = username
        user = userRepository.save(user)
        return user.toDTO()
    }
}
