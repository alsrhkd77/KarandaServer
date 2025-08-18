package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.ReportStatusDTO
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.enums.ReportTarget
import org.jetbrains.annotations.NotNull
import java.time.ZonedDateTime

@Entity(name = "report_status")
class ReportStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    val target: ReportTarget,

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    val region: BDORegion,

    @Column(length = 50)
    val channel: String? = null,    // null = 전체 채널

    @NotNull
    var active: Boolean, // true = 출현/활성, false = 사망/비활성

    @Column(nullable = false)
    var eventTime: ZonedDateTime,

    @Column(nullable = false)
    var updatedAt: ZonedDateTime,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    var sourceReport: Report,
) {
    fun toDTO(): ReportStatusDTO {
        return ReportStatusDTO(
            id = id,
            target = target,
            region = region,
            channel = channel,
            active = active,
            eventTime = eventTime,
            updatedAt = updatedAt,
            sourceReport = sourceReport.toDTO()
        )
    }
}