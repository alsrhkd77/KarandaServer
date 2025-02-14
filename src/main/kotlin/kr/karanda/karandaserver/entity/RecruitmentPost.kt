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
    @NotNull
    var title: String,
    var region: String,
    var createdAt: ZonedDateTime,
    var category: String,
    var subcategory: String,
    var status: Boolean,
    var recruitMethod: String,
    var maximumParticipants: Int,
    var guildName: String? = null,
    @Column(length = 1024)
    var content: String? = null,
    var discordLink: String? = null,
    var showContentAfterJoin: Boolean? = null,
    var blinded: Boolean,

    @ManyToOne
    var author: User,

    @OneToMany(mappedBy = "post")
    val applicants: List<Applicant> = mutableListOf(),
) {
    fun toDTO(): RecruitmentPostDTO {
        return RecruitmentPostDTO(
            id = id,
            title = title,
            region = region,
            createdAt = createdAt,
            category = category,
            subcategory = subcategory,
            status = status,
            recruitMethod = recruitMethod,
            currentParticipants = applicants.count { it.canceledAt == null && it.rejectedAt == null && it.reason == null },
            maximumParticipants = maximumParticipants,
            guildName = guildName,
            content = content,
            discordLink = discordLink,
            showContentAfterJoin = showContentAfterJoin,
            blinded = blinded,
            author = author.toUserDTO(),
        )
    }
}