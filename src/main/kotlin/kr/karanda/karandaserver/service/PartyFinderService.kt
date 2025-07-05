package kr.karanda.karandaserver.service

import com.google.firebase.messaging.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.BroadcastMessage
import kr.karanda.karandaserver.dto.NotificationMessage
import kr.karanda.karandaserver.exception.InvalidArgumentException
import kr.karanda.karandaserver.dto.Applicant as ApplicantDTO
import kr.karanda.karandaserver.entity.Applicant as ApplicantEntity
import kr.karanda.karandaserver.dto.RecruitmentPost as RecruitmentPostDTO
import kr.karanda.karandaserver.entity.RecruitmentPost as RecruitmentPostEntity
import kr.karanda.karandaserver.exception.UnknownUserException
import kr.karanda.karandaserver.infrastructure.redis.RedisPublisher
import kr.karanda.karandaserver.repository.jpa.ApplicantRepository
import kr.karanda.karandaserver.repository.jpa.RecruitmentPostRepository
import kr.karanda.karandaserver.repository.jpa.UserFcmSettingsRepository
import kr.karanda.karandaserver.repository.jpa.UserRepository
import kr.karanda.karandaserver.util.RandomCodeUtils
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class PartyFinderService(
    val recruitmentPostRepository: RecruitmentPostRepository,
    val userRepository: UserRepository,
    val applicantRepository: ApplicantRepository,
    val userFcmSettingsRepository: UserFcmSettingsRepository,
    val redisPublisher: RedisPublisher,
) {
    /* return post id */
    fun createNewPost(uuid: String, data: RecruitmentPostDTO): RecruitmentPostDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        var post = RecruitmentPostEntity(
            title = data.title,
            region = data.region,
            createdAt = ZonedDateTime.now(ZoneId.of("UTC")),
            category = data.category,
            recruitmentType = data.recruitmentType,
            maxMembers = data.maxMembers,
            guildName = data.guildName,
            content = data.content,
            privateContent = data.privateContent,
            discordLink = data.discordLink,
            blinded = false,
            author = user
        )
        post = recruitmentPostRepository.saveAndFlush(post)
        //publishLatestPost(post.toDTO())
        redisPublisher.broadcast(BroadcastMessage.updateRecruitmentPost(post.toDTO()))

        return post.toDTO()
    }

    fun updatePost(uuid: String, data: RecruitmentPostDTO): RecruitmentPostDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        val post = data.id?.let { recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = it, uuid = user.userUUID) }
            ?: throw InvalidArgumentException()
        post.apply {
            title = data.title
            maxMembers = data.maxMembers
            specLimit = data.specLimit
            content = data.content
            privateContent = data.privateContent
            discordLink = data.discordLink
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        }
        recruitmentPostRepository.saveAndFlush(post)
        //publishLatestPost(post.toDTO())
        redisPublisher.broadcast(BroadcastMessage.updateRecruitmentPost(post.toDTO()))
        return post.toDTO()
    }

    fun getPosts(region: String): List<RecruitmentPostDTO> {
        return recruitmentPostRepository.findTop100ByRegion(region).map { it.toDTO() }
    }

    fun getPost(postId: Long, uuid: String? = null): RecruitmentPostDTO {
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw InvalidArgumentException()
        if (uuid != null) {
            val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
            if (post.author.id == user.id) {
                return post.toDTO()
            }
            if (post.applicants.any { it.owner.id == user.id }) {
                val applicant = post.applicants.first { it.owner.id == user.id }.toDTO()
                if (applicant.acceptedAt != null && applicant.cancelledAt == null && applicant.rejectedAt == null) {
                    return post.toDTO()
                }
            }
        }
        return post.toDTO().apply {
            privateContent = ""
        }
    }

    fun getPostApplicant(postId: Long, uuid: String): ApplicantDTO? {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        return applicantRepository.findByPost_IdAndOwner_Id(postId = postId, ownerId = user.id!!)?.toDTO()
    }

    fun getPostApplicants(postId: Long, uuid: String): List<ApplicantDTO> {
        val post = recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = postId, uuid = uuid) ?: throw Exception()
        return post.applicants.map { it.toDTO() }
    }

    fun fetchUserJoined(uuid: String): List<ApplicantDTO> {
        val applicants = applicantRepository.findTop100ByOwner_UserUUID(uuid = uuid)
        return applicants.map { it.toDTO() }
    }

    fun openPost(postId: Long, uuid: String): RecruitmentPostDTO {
        val posts = userRepository.findByUserUUID(uuid)?.recruitmentPost ?: throw UnknownUserException()
        val post = posts.singleOrNull { it.id == postId } ?: throw InvalidArgumentException()
        if (posts.any { it.status } || post.status) {
            // Cannot open multiple posts OR Post already opened
            return post.toDTO()
        } else {
            val needNotify = post.openedAt == null
            post.openedAt = ZonedDateTime.now(ZoneId.of("UTC"))
            recruitmentPostRepository.saveAndFlush(post)

            val broadcastMessages = mutableListOf(BroadcastMessage.updateRecruitmentPost(post.toDTO()))
            if (needNotify) {
                broadcastMessages.add(BroadcastMessage.notifyRecruitmentPost(post.toDTO()))
                if (post.category == "guildBossRaid") {
                    val fcm =
                        userFcmSettingsRepository.findAllByRegionAndPartyFinderIsTrue(post.region).map { it.toDTO() }
                    val fcmMessage = MulticastMessage.builder()
                        .addAllTokens(fcm.map { it.token })
                        .setAndroidConfig(
                            AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(
                                    AndroidNotification.builder()
                                        .setTitle("Party Finder")
                                        .setTitleLocalizationKey("partyFinder")
                                        .setBody("${post.title} - Now recruiting!")
                                        .setBodyLocalizationKey("guildBossRaidRecruitOpened")
                                        .addBodyLocalizationArg(post.title)
                                        .build()
                                ).build()
                        ).setWebpushConfig(
                            WebpushConfig.builder()
                                .setFcmOptions(WebpushFcmOptions.builder().setLink("https://www.karanda.kr").build())
                                .setNotification(
                                    WebpushNotification.builder()
                                        .setTitle("Party Finder")
                                        .setBody("${post.title} - Now recruiting!")
                                        .setIcon("https://www.karanda.kr/icons/android-chrome-512x512.png")
                                        .setBadge("https://www.karanda.kr/icons/android-chrome-192x192.png")
                                        .build()
                                ).build()
                        ).build()
                    FirebaseMessaging.getInstance().sendEachForMulticast(fcmMessage)
                }
            }
            redisPublisher.broadcast(broadcastMessages)
            //publishLatestPost(post.toDTO())

            return post.toDTO()
        }
    }

    fun closePost(postId: Long, uuid: String): RecruitmentPostDTO {
        val post = recruitmentPostRepository.findByIdAndAuthor_UserUUID(id = postId, uuid = uuid)
            ?: throw InvalidArgumentException()
        post.closedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        recruitmentPostRepository.saveAndFlush(post)

        // Notify
        //publishLatestPost(post.toDTO())
        redisPublisher.broadcast(BroadcastMessage.updateRecruitmentPost(post.toDTO()))
        return post.toDTO()
    }

    fun joinToPost(postId: Long, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
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
                    joinAt = ZonedDateTime.now(ZoneId.of("UTC")),
                    post = post,
                    owner = user
                )
            )

            //Notify firestore
            redisPublisher.broadcast(
                BroadcastMessage.updateApplicantStatus(
                    authorUUID = post.author.userUUID,
                    userUUID = uuid,
                    postId = postId,
                    applicant = applicant.toDTO()
                )
            )
            return applicant.toDTO()
        }
    }

    fun cancelToPost(postId: Long, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        val applicant =
            applicantRepository.findByPost_IdAndOwner_Id(postId = postId, ownerId = user.id!!) ?: throw Exception()
        if (applicant.cancelledAt != null) {
            // Already canceled
            return applicant.toDTO()
        } else {
            applicant.cancelledAt = ZonedDateTime.now(ZoneId.of("UTC"))
            applicantRepository.save(applicant)

            //Notify
            redisPublisher.broadcast(
                listOf(
                    BroadcastMessage.updateApplicantStatus(
                        authorUUID = applicant.post.author.userUUID,
                        userUUID = uuid,
                        postId = postId,
                        applicant = applicant.toDTO()
                    ),
                    BroadcastMessage.updateRecruitmentPostDetail(applicant.post.toDTO())
                )
            )
            return applicant.toDTO()
        }
    }

    fun acceptApplicant(postId: Long, applicantId: String, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw Exception()
        val applicant = applicantRepository.findByPost_IdAndOwner_DiscordIdAndPost_Author_Id(
            postId = postId,
            ownerDiscordId = applicantId,
            postAuthorId = user.id!!
        ) ?: throw Exception()
        if (applicant.cancelledAt != null) {
            return applicant.toDTO()
        } else if (post.maxMembers <= post.applicants.count { it.cancelledAt == null && it.rejectedAt == null }) {
            return applicant.toDTO()
        }
        applicant.acceptedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        applicant.rejectedAt = null
        if (applicant.code == null) {
            applicant.code = generateReservationCode(postId = postId)
        }
        applicantRepository.saveAndFlush(applicant)

        //Notify
        val fcm = userFcmSettingsRepository.findAllByOwner_UserUUID(applicant.owner.userUUID).map { it.toDTO() }
        val fcmMessage = MulticastMessage.builder()
            .addAllTokens(fcm.map { it.token })
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setTitle("Party Finder")
                            .setTitleLocalizationKey("partyFinder")
                            .setBody("${post.title} - You got in!")
                            .setBodyLocalizationKey("recruitEntryAccepted")
                            .addBodyLocalizationArg(post.title)
                            .build()
                    ).build()
            ).setWebpushConfig(
                WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.builder().setLink("https://www.karanda.kr").build())
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle("Party Finder")
                            .setBody("${post.title} - You got in!")
                            .setIcon("https://www.karanda.kr/icons/android-chrome-512x512.png")
                            .setBadge("https://www.karanda.kr/icons/android-chrome-192x192.png")
                            .build()
                    ).build()
            ).build()
        FirebaseMessaging.getInstance().sendEachForMulticast(fcmMessage)
        //publishLatestPost(applicant.post.toDTO())
        redisPublisher.broadcast(
            listOf(
                BroadcastMessage.updateApplicantStatus(
                    authorUUID = applicant.post.author.userUUID,
                    userUUID = uuid,
                    postId = postId,
                    applicant = applicant.toDTO()
                ),
                BroadcastMessage.updateRecruitmentPostDetail(applicant.post.toDTO()),
                BroadcastMessage(
                    destinations = mutableListOf("/live-data/${applicant.owner.userUUID}/notification/private"),
                    message = Json.encodeToString(
                        NotificationMessage(
                            feature = "notifications",
                            contentsKey = "recruitEntryAccepted",
                            contentsArgs = listOf(post.title),
                            route = "/party-finder/recruit/${post.id}",
                            mdContents = false
                        )
                    ),
                ),
            )
        )
        return applicant.toDTO()
    }

    fun rejectApplicant(postId: Long, applicantId: String, uuid: String): ApplicantDTO {
        val user = userRepository.findByUserUUID(uuid) ?: throw UnknownUserException()
        val post = recruitmentPostRepository.findByIdOrNull(postId) ?: throw Exception()
        val applicant = applicantRepository.findByPost_IdAndOwner_DiscordIdAndPost_Author_Id(
            postId = post.id!!,
            ownerDiscordId = applicantId,
            postAuthorId = user.id!!
        ) ?: throw Exception()
        if (applicant.cancelledAt != null) {
            return applicant.toDTO()
        }
        applicant.rejectedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        applicant.acceptedAt = null
        applicantRepository.saveAndFlush(applicant)

        val fcm = userFcmSettingsRepository.findAllByOwner_UserUUID(applicant.owner.userUUID).map { it.toDTO() }
        val fcmMessage = MulticastMessage.builder()
            .addAllTokens(fcm.map { it.token })
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setTitle("PartyFinder")
                            .setTitleLocalizationKey("partyFinder")
                            .setBody("${post.title} - Maybe next time!")
                            .setBodyLocalizationKey("recruitEntryRejected")
                            .addBodyLocalizationArg(post.title)
                            .build()
                    ).build()
            ).setWebpushConfig(
                WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.builder().setLink("https://www.karanda.kr").build())
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle("Notifications")
                            .setBody("${post.title} - Maybe next time!")
                            .setIcon("https://www.karanda.kr/icons/android-chrome-512x512.png")
                            .setBadge("https://www.karanda.kr/icons/android-chrome-192x192.png")
                            .build()
                    ).build()
            ).build()
        FirebaseMessaging.getInstance().sendEachForMulticast(fcmMessage)
        redisPublisher.broadcast(
            listOf(
                BroadcastMessage.updateApplicantStatus(
                    authorUUID = applicant.post.author.userUUID,
                    userUUID = uuid,
                    postId = postId,
                    applicant = applicant.toDTO()
                ),
                BroadcastMessage.updateRecruitmentPostDetail(applicant.post.toDTO()),
                BroadcastMessage(
                    destinations = mutableListOf("/live-data/${applicant.owner.userUUID}/notification/private"),
                    message = Json.encodeToString(
                        NotificationMessage(
                            feature = "notifications",
                            contentsKey = "recruitEntryRejected",
                            contentsArgs = listOf(post.title),
                            route = "/party-finder/recruit/${post.id}",
                            mdContents = false
                        )
                    ),
                )
            )
        )
        return applicant.toDTO()
    }

    /*private fun publishLatestPost(post: RecruitmentPostDTO) {
        synchronizationDataRepository.broadcast(
            listOf(
                BroadcastMessage(
                    destinations = mutableListOf(
                        "/live-data/adventurer-hub/${post.region}/post",
                        "/live-data/adventurer-hub/post/${post.id}"
                    ),
                    message = Json.encodeToString(post.simplify())
                )
            )
        )
    }*/

    private fun generateReservationCode(postId: Long): String {
        val code = RandomCodeUtils().generate(5)
        return if (applicantRepository.existsByCodeAndPost_Id(code, postId)) {
            generateReservationCode(postId)
        } else {
            code
        }
    }
}