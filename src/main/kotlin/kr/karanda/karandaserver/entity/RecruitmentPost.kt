package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime
import kr.karanda.karandaserver.dto.RecruitmentPost as RecruitmentPostDTO

@Entity(name = "recruitment_post")
class RecruitmentPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var region: String,
    @NotNull
    var title: String,
    var category: String,
    var recruitmentType: String,
    var maxMembers: Int,
    var guildName: String = "",
    var specLimit: Int? = null,
    @Column(length = 1024)
    var content: String = "",
    @Column(length = 1024)
    var privateContent: String = "",
    var discordLink: String? = null,
    var createdAt: ZonedDateTime,
    var updatedAt: ZonedDateTime? = null,
    var blinded: Boolean,
    var openedAt: ZonedDateTime? = null,
    var closedAt: ZonedDateTime? = null,

    @ManyToOne
    var author: User,

    @OneToMany(mappedBy = "post")
    val applicants: List<Applicant> = mutableListOf(),
) {
    @get:Transient
    val status: Boolean
        get() = !(openedAt == null || (closedAt != null && openedAt!!.isBefore(closedAt)))

    fun toDTO(): RecruitmentPostDTO {
        return RecruitmentPostDTO(
            id = id,
            region = region,
            title = title,
            category = category,
            status = status,
            recruitmentType = recruitmentType,
            maxMembers = maxMembers,
            guildName = guildName,
            specLimit = specLimit,
            content = content,
            privateContent = privateContent,
            discordLink = discordLink,
            createdAt = createdAt,
            updatedAt = updatedAt,
            blinded = blinded,
            author = author.toUserDTO(),
            currentParticipants = applicants.count { it.cancelledAt == null && it.rejectedAt == null && it.acceptedAt != null },
        )
    }
}