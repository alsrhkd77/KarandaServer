package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportDTO
import kr.karanda.karandaserver.enums.BDORegion
import org.jetbrains.annotations.NotNull
import java.time.ZonedDateTime

@Entity(name = "report")
class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(length = 50, nullable = false)
    val target: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    val region: BDORegion,

    @Column(length = 50)
    val channel: String? = null,    // null = 전체 채널

    @NotNull
    var active: Boolean, // true = 출현/활성, false = 사망/비활성

    @Column(nullable = false)
    val eventAt: ZonedDateTime,

    @Column(nullable = false)
    val reportedAt: ZonedDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    var reporter: User? = null,

    var isCanceled: Boolean = false,

    @Column(length = 1024)
    var cancelReason: String? = null
) {
    fun toDTO(): ReportDTO {
        return ReportDTO(
            id = id,
            target = target,
            region = region,
            channel = channel,
            active = active,
            eventAt = eventAt,
            reportedAt = reportedAt,
            reporter = reporter?.toUserDTO(),
            isCanceled = isCanceled,
            cancelReason = cancelReason
        )
    }
}