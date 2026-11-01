package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.enums.BDORegion

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

        /** Reporter Hub 상태 델타. 스냅샷과 같은 형태이며 클라이언트가 행별 version으로 병합한다. */
        fun reportHubStatus(region: BDORegion, status: ReportHubStatusDTO): BroadcastMessage = BroadcastMessage(
            destinations = mutableListOf("/live-data/report-hub/${region.name}/status"),
            message = Json.encodeToString(status)
        )

        /** Reporter Hub 알림 이벤트(Windows용). 수신 여부는 클라이언트가 팔로우·설정으로 판단한다. */
        fun reportHubAlert(region: BDORegion, alert: ReportHubAlertDTO): BroadcastMessage = BroadcastMessage(
            destinations = mutableListOf("/live-data/report-hub/${region.name}/alert"),
            message = Json.encodeToString(alert)
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
