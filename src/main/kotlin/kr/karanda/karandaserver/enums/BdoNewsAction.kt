package kr.karanda.karandaserver.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BdoNewsAction {
    @SerialName("CREATED")
    CREATED,

    @SerialName("UPDATED")
    UPDATED,
}
