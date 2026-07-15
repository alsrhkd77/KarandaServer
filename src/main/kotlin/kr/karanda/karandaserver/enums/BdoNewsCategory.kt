package kr.karanda.karandaserver.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BdoNewsCategory {
    @SerialName("NOTICE")
    NOTICE,

    @SerialName("UPDATE")
    UPDATE,

    @SerialName("EVENT")
    EVENT,

    @SerialName("LAB")
    LAB,
}
