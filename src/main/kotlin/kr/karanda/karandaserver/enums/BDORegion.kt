package kr.karanda.karandaserver.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZoneId

@Serializable
enum class BDORegion(val timezone: ZoneId) {
    @SerialName("KR")
    KR(ZoneId.of("Asia/Seoul")),

    @SerialName("NA")
    NA(ZoneId.of("UTC")),

    @SerialName("EU")
    EU(ZoneId.of("UTC"))
}