package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class SimplifiedRecruitmentPost(
    var id: Long?,
    var title: String,
    var region: String,
    var category: String,
    var status: Boolean,
    var recruitmentType: String,
    var maxMembers: Int,
    var guildName: String,
    //var contents: String?,
    //var discordLink: String?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    var createdAt: ZonedDateTime?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    var updatedAt: ZonedDateTime?,
    var blinded: Boolean = false,
    var author: UserDTO,
    var currentParticipants: Int,
)
