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
    return !(this.year != other.year || this.monthValue != other.monthValue || this.dayOfMonth != other.dayOfMonth)
}

fun ZonedDateTime.difference(other: ZonedDateTime): Duration {
    return Duration.between(this.toInstant(), other.toInstant())
}
