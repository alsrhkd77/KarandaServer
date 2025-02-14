package kr.karanda.karandaserver.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.BroadcastMessage
import kr.karanda.karandaserver.dto.Applicant as ApplicantDTO
import kr.karanda.karandaserver.entity.Applicant as ApplicantEntity
import kr.karanda.karandaserver.dto.RecruitmentPost as RecruitmentPostDTO
import kr.karanda.karandaserver.entity.RecruitmentPost as RecruitmentPostEntity
import kr.karanda.karandaserver.exception.UnknownUser
import kr.karanda.karandaserver.repository.jpa.ApplicantRepository
import kr.karanda.karandaserver.repository.jpa.RecruitmentPostRepository
import kr.karanda.karandaserver.repository.SynchronizationDataRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.RandomCodeFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class AdventurerHubService(
    val recruitmentPostRepository: RecruitmentPostRepository,
    val userRepository: UserRepository,
    val applicantRepository: ApplicantRepository,
    val synchronizationDataRepository: SynchronizationDataRepository,
) {
    /* return post id */
    fun createNewPost(uuid: String, data: RecruitmentPostDTO): RecruitmentPostDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        var post = RecruitmentPostEntity(
            title = data.title,
            region = data.region,
            createdAt = ZonedDateTime.now(ZoneId.of("UTC")),
            category = data.category,
            subcategory = data.subcategory ?: "",
            status = false,
            recruitMethod = data.recruitMethod,
            maximumParticipants = data.maximumParticipants,
            guildName = data.guildName,
            content = data.content,
            discordLink = data.discordLink,
            showContentAfterJoin = data.showContentAfterJoin,
            blinded = false,
            author = user
        )
        post = recruitmentPostRepository.saveAndFlush(post)

        return post.toDTO()
    }

    fun updatePost(uuid: String, data: RecruitmentPostDTO): RecruitmentPostDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val post = data.id?.let { recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = it, uuid = user.userUUID) }
            ?: throw Exception("Post not found")
        post.title = data.title
        post.content = data.content
        post.discordLink = data.discordLink
        post.maximumParticipants = data.maximumParticipants
        post.showContentAfterJoin = data.showContentAfterJoin
        recruitmentPostRepository.saveAndFlush(post)

        synchronizationDataRepository.broadcast(
            BroadcastMessage(
                destinations = mutableListOf("/live-data/${post.region}/adventurer-hub/post"),
                message = Json.encodeToString(post.toDTO().simplify())
            )
        )
        return post.toDTO()
    }

    fun getPosts(region: String): List<RecruitmentPostDTO> {
        return recruitmentPostRepository.findTop100ByRegion(region).map { it.toDTO() }
    }

    fun getPost(postId: Long, uuid: String? = null): RecruitmentPostDTO {
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw Exception()
        val result = post.toDTO()
        if (uuid != null) {
            val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
            if (post.applicants.any { it.owner.id == user.id }) {
                val applicant = post.applicants.first { it.owner.id == user.id }.toDTO()
                result.applicant = applicant
                if (post.showContentAfterJoin == true && applicant.approvedAt != null && applicant.canceledAt == null && applicant.rejectedAt == null) {
                    return result
                }
            } else if (post.author.id == user.id) {
                return result
            }
        }
        if (post.showContentAfterJoin == true) {
            result.content = ""
            result.discordLink = ""
        }
        return result
    }

    fun getPostApplicant(postId: Long, uuid: String): ApplicantDTO? {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        return applicantRepository.findByPost_IdAndOwner_Id(postId = postId, ownerId = user.id!!)?.toDTO()
    }

    fun getPostApplicants(postId: Long, uuid: String): List<ApplicantDTO> {
        val post = recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = postId, uuid = uuid) ?: throw Exception()
        return post.applicants.map { it.toDTO() }
    }

    fun getUserApplied(uuid: String): List<ApplicantDTO> {
        val applicants = applicantRepository.findTop100ByOwner_UserUUID(uuid = uuid)
        return applicants.map { it.toDTO() }
    }

    fun openPost(postId: Long, uuid: String): RecruitmentPostDTO {
        val posts = userRepository.findByUserUUID(uuid)?.recruitmentPost ?: throw UnknownUser()
        //if (posts.any { it.status }) {
        if (false) {
            // Cannot open multiple posts
            throw Exception()
        } else {
            val post = posts.singleOrNull { it.id == postId } ?: throw Exception()
            if (post.status) {
                // Post already opened
                return post.toDTO()
            } else {
                post.status = true
                recruitmentPostRepository.saveAndFlush(post)

                // Notify firestore
                synchronizationDataRepository.broadcast(
                    BroadcastMessage(
                        destinations = mutableListOf("/live-data/${post.region}/adventurer-hub/post"),
                        message = Json.encodeToString(post.toDTO().simplify())
                    )
                )
                return post.toDTO()
            }
        }
    }

    fun closePost(postId: Long, uuid: String): RecruitmentPostDTO {
        val post = recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = postId, uuid = uuid) ?: throw Exception()
        post.status = false
        recruitmentPostRepository.saveAndFlush(post)

        // Notify firestore
        synchronizationDataRepository.broadcast(
            BroadcastMessage(
                destinations = mutableListOf("/live-data/${post.region}/adventurer-hub/post"),
                message = Json.encodeToString(post.toDTO().simplify())
            )
        )
        return post.toDTO()
    }

    fun applyToPost(postId: Long, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw Exception()

        if (!post.status) {
            //Post is closed
            throw Exception()
        } else if (post.applicants.any { it.owner.id == user.id }) {
            //Already applied
            throw Exception()
        } else if (post.author.id == user.id) {
            //Owner cannot apply -> bad request
            throw Exception()
        } else {
            val applicant = applicantRepository.saveAndFlush(
                ApplicantEntity(
                    appliedAt = ZonedDateTime.now(ZoneId.of("UTC")),
                    post = post,
                    owner = user
                )
            )

            //Notify firestore
            synchronizationDataRepository.broadcast(
                BroadcastMessage(
                    destinations = mutableListOf("/live-data/${post.author.userUUID}/adventurer-hub/applicant"),
                    message = Json.encodeToString(applicant.toDTO()),
                )
            )
            return applicant.toDTO()
        }
    }

    fun cancelToPost(postId: Long, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val applicant =
            applicantRepository.findByPost_IdAndOwner_Id(postId = postId, ownerId = user.id!!) ?: throw Exception()
        if (applicant.canceledAt != null) {
            // Already canceled
            throw Exception()
        } else {
            applicant.canceledAt = ZonedDateTime.now(ZoneId.of("UTC"))
            applicantRepository.save(applicant)

            //Notify firestore
            synchronizationDataRepository.broadcast(
                BroadcastMessage(
                    destinations = mutableListOf("/live-data/${applicant.post.author.userUUID}/adventurer-hub/applicant"),
                    message = Json.encodeToString(applicant),
                )
            )
            synchronizationDataRepository.broadcast(
                BroadcastMessage(
                    destinations = mutableListOf("/live-data/${applicant.post.region}/adventurer-hub/post"),
                    message = Json.encodeToString(applicant.post.toDTO().simplify())
                )
            )
            return applicant.toDTO()
        }
    }

    fun approveApplicant(postId: Long, applicantId: String, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw Exception()
        val applicant = applicantRepository.findByPost_IdAndOwner_DiscordIdAndPost_Author_Id(
            postId = postId,
            ownerDiscordId = applicantId,
            postAuthorId = user.id!!
        ) ?: throw Exception()
        if (applicant.canceledAt != null) {
            return applicant.toDTO()
        } else if (post.maximumParticipants <= post.applicants.count { it.canceledAt == null && it.rejectedAt == null && it.reason == null }) {
            return applicant.toDTO()
        }
        applicant.approvedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        applicant.code = generateReservationCode(postId = postId)
        applicantRepository.saveAndFlush(applicant)

        //Notify firestore
        synchronizationDataRepository.broadcast(
            BroadcastMessage(
                destinations = mutableListOf("/live-data/${applicant.owner.userUUID}/adventurer-hub/applicant"),
                message = Json.encodeToString(applicant),
            )
        )
        synchronizationDataRepository.broadcast(
            BroadcastMessage(
                destinations = mutableListOf("/live-data/${applicant.post.region}/adventurer-hub/post"),
                message = Json.encodeToString(applicant.post.toDTO().simplify())
            )
        )
        return applicant.toDTO()
    }

    fun rejectApplicant(postId: Long, applicantId: String, reason: String, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUser()
        val applicant = applicantRepository.findByPost_IdAndOwner_DiscordIdAndPost_Author_Id(
            postId = postId,
            ownerDiscordId = applicantId,
            postAuthorId = user.id!!
        ) ?: throw Exception()
        if (applicant.canceledAt != null) {
            return applicant.toDTO()
        }
        applicant.rejectedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        applicant.reason = reason
        applicantRepository.saveAndFlush(applicant)

        //Notify firestore
        synchronizationDataRepository.broadcast(
            BroadcastMessage(
                destinations = mutableListOf("/live-data/${applicant.owner.userUUID}/adventurer-hub/applicant"),
                message = Json.encodeToString(applicant),
            )
        )
        return applicant.toDTO()
    }

    private fun generateReservationCode(postId: Long): String {
        val code = RandomCodeFactory().generate(5)
        return if (applicantRepository.existsByCodeAndPost_Id(code, postId)) {
            generateReservationCode(postId)
        } else {
            code
        }
    }
}