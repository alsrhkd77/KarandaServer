package kr.karanda.karandaserver.entity

import jakarta.persistence.*

/**
 * 같은 지역·대상(또는 지역 점검)의 제보 처리를 직렬화하기 위한 락 행.
 * 회차 시작처럼 "없으면 만든다" 판단이 동시에 일어나면 중복 생성되므로 이 행을 먼저 `FOR UPDATE`로 잡는다.
 */
@Entity
@Table(name = "report_hub_lock")
class ReportHubLock(
    @Id
    @Column(name = "lock_key", length = 64)
    val lockKey: String,
)
