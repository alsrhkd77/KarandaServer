package kr.karanda.karandaserver.repository.jpa

import jakarta.persistence.LockModeType
import kr.karanda.karandaserver.entity.ReportHubLock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReportHubLockRepository : JpaRepository<ReportHubLock, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ReportHubLock l where l.lockKey = :key")
    fun lock(@Param("key") key: String): ReportHubLock?
}
