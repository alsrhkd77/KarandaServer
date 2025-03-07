package kr.karanda.karandaserver.enums

import java.time.ZoneId

enum class BDORegion(val timezone: ZoneId) {
    KR(ZoneId.of("Asia/Seoul")), NA(ZoneId.of("UTC")), EU(ZoneId.of("UTC"))
}