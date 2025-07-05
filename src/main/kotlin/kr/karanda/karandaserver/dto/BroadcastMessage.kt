package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BroadcastMessage(
    var destinations: MutableList<String> = mutableListOf(),
    var message: String = ""
) {
    companion object Factory {
        fun updateRecruitmentPost(post: RecruitmentPost): BroadcastMessage = updateRecruitmentPostDetail(post).apply {
            destinations.add("/live-data/party-finder/${post.region}/post")
        }

        fun updateRecruitmentPostDetail(post: RecruitmentPost): BroadcastMessage = BroadcastMessage(
            destinations = mutableListOf("/live-data/party-finder/post/${post.id}"),
            message = Json.encodeToString(post.simplify())
        )

        fun notifyRecruitmentPost(post: RecruitmentPost): BroadcastMessage = BroadcastMessage(
            destinations = mutableListOf("/live-data/${post.region}/notification"),
            message = Json.encodeToString(
                NotificationMessage(
                    feature = "partyFinder",
                    contentsKey = "${post.category}RecruitOpened",
                    contentsArgs = listOf(post.title),
                    mdContents = false,
                    route = "/party-finder/recruit/${post.id}"
                )
            )
        )

        fun updateApplicantStatus(
            authorUUID: String,
            userUUID: String,
            postId: Long,
            applicant: Applicant
        ): BroadcastMessage = BroadcastMessage(
            destinations = mutableListOf(
                "/live-data/${userUUID}/party-finder/applicants/private",
                "/live-data/${authorUUID}/party-finder/post/${postId}/applicants/private"
            ),
            message = Json.encodeToString(applicant),
        )
    }
}
