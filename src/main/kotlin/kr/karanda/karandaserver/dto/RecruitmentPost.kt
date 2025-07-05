package kr.karanda.karandaserver.dto

import java.time.ZonedDateTime

data class RecruitmentPost(
    var id: Long?,
    var region: String,
    var title: String,
    var category: String,
    var status: Boolean,
    var recruitmentType: String,
    var maxMembers: Int,
    var guildName: String,
    var specLimit: Int? = null,
    var content: String,
    var privateContent: String,
    var discordLink: String?,
    var createdAt: ZonedDateTime?,
    var updatedAt: ZonedDateTime?,
    var blinded: Boolean = false,
    var author: UserDTO?,
    var currentParticipants: Int = 0,
) {
    fun simplify(): SimplifiedRecruitmentPost {
        return SimplifiedRecruitmentPost(
            id = this.id,
            title = this.title,
            region = this.region,
            category = this.category,
            status = this.status,
            recruitmentType = this.recruitmentType,
            maxMembers = this.maxMembers,
            guildName = this.guildName,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            blinded = this.blinded,
            author = this.author!!,
            currentParticipants = this.currentParticipants,
        )
    }
}
