package kr.karanda.karandaserver.util

import java.time.Duration
import java.time.ZonedDateTime

fun ZonedDateTime.toMidnight(): ZonedDateTime {
    return ZonedDateTime.of(
        this.year,
        this.monthValue,
        this.dayOfMonth,
        0, 0, 0, 0, this.zone
    )
}

fun ZonedDateTime.isSameDayAs(other: ZonedDateTime): Boolean {
    val zoned = other.withZoneSameInstant(this.zone)
    return !(this.year != zoned.year || this.monthValue != zoned.monthValue || this.dayOfMonth != zoned.dayOfMonth)
}

fun ZonedDateTime.difference(other: ZonedDateTime): Duration {
    return Duration.between(this.toInstant(), other.toInstant())
}
