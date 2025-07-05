package kr.karanda.karandaserver.repository.jpa

import kr.karanda.karandaserver.entity.BDOFamily
import kr.karanda.karandaserver.entity.BDOFamilyVerification
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface BDOFamilyVerificationRepository : JpaRepository<BDOFamilyVerification, Long> {
    fun findByFamilyAndStartPointAndCreatedAtAfterOrderByCreatedAtDesc(
        family: BDOFamily,
        startPoint: Boolean,
        createdAtAfter: ZonedDateTime
    ): BDOFamilyVerification?

    fun findAllByFamilyAndStartPointAndCreatedAtAfterOrderByCreatedAtDesc(
        family: BDOFamily,
        startPoint: Boolean,
        createdAtAfter: ZonedDateTime
    ): List<BDOFamilyVerification>
}