package kr.karanda.karandaserver.util

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 자정으로 변환.
 *
 * `hour`, `minute`, `second`, `nanoOfSecond`를 0으로 바꿈.
 * 나머지는 원본과 동일
 *
 * @return [ZonedDateTime]
 */
fun ZonedDateTime.toMidnight(): ZonedDateTime {
    return ZonedDateTime.of(
        this.year,
        this.monthValue,
        this.dayOfMonth,
        0, 0, 0, 0, this.zone
    )
}

/**
 * [other]과 같은 날인지 판별.
 * 같은 날이면 `true` 반환.
 *
 * @param [other] [ZonedDateTime]
 * @return [Boolean]
 */
fun ZonedDateTime.isSameDayAs(other: ZonedDateTime): Boolean {
    val zoned = other.withZoneSameInstant(this.zone)
    return !(this.year != zoned.year || this.monthValue != zoned.monthValue || this.dayOfMonth != zoned.dayOfMonth)
}

/**
 * [other]와 시간 차이를 계산.
 *
 * @param [other] [ZonedDateTime]
 * @return [Duration]
 */
fun ZonedDateTime.difference(other: ZonedDateTime): Duration {
    val zoned = other.withZoneSameInstant(this.zone)
    return Duration.between(this.toInstant(), zoned.toInstant())
}
