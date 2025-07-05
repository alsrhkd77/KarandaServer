package kr.karanda.karandaserver.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationMessage(
    val feature: String,
    val contentsKey: String,
    var contentsArgs: List<String>? = null,
    var route: String? = null,
    val mdContents: Boolean,
)
