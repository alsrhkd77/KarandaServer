package kr.karanda.karandaserver.data

import kotlinx.serialization.Serializable
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.util.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class SimplifiedRecruitmentPost(
    var id: Long?,
    var title: String,
    var region: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    var createdAt: ZonedDateTime?,
    var category: String,
    var subcategory: String?,
    var status: Boolean,
    var recruitMethod: String,
    var currentParticipants: Int,
    var maximumParticipants: Int,
    var guildName: String?,
    //var contents: String?,
    //var discordLink: String?,
    var showContentAfterJoin: Boolean?,
    var blinded: Boolean = false,
    var author: User,
)
