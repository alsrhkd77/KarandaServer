package kr.karanda.karandaserver.dto

import jakarta.persistence.Column
import kr.karanda.karandaserver.data.SimplifiedRecruitmentPost
import java.time.ZonedDateTime

data class RecruitmentPost(
    var id: Long?,
    var title: String,
    var region: String,
    var createdAt: ZonedDateTime?,
    var category: String,
    var subcategory: String?,
    var status: Boolean,
    var recruitMethod: String,
    var currentParticipants: Int = 0,
    var maximumParticipants: Int,
    var guildName: String?,
    @Column(length = 1024)
    var content: String?,
    var discordLink: String?,
    var showContentAfterJoin: Boolean?,
    var blinded: Boolean = false,
    var author: User?,
    var applicant: Applicant? = null,
) {
    fun simplify(): SimplifiedRecruitmentPost {
        return SimplifiedRecruitmentPost(
            id = this.id,
            title = this.title,
            region = this.region,
            createdAt = this.createdAt,
            category = this.category,
            subcategory = this.subcategory,
            status = this.status,
            recruitMethod = this.recruitMethod,
            currentParticipants = this.currentParticipants,
            maximumParticipants = this.maximumParticipants,
            guildName = this.guildName,
            showContentAfterJoin = this.showContentAfterJoin,
            blinded = this.blinded,
            author = this.author!!,
        )
    }
}
