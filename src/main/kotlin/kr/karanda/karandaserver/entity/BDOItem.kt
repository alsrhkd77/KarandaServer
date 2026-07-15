package kr.karanda.karandaserver.entity

import jakarta.persistence.*
import kr.karanda.karandaserver.dto.BDOItem as BDOItemDTO

@Entity(name = "bdo_item")
class BDOItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    @Column(unique = true)
    var itemNum: Int,
    var itemNameKr: String? = null,
    var itemNameEn: String? = null,
    var maxEnhancementLevel: Int? = null,
    var grade: Int? = null,
    // 거래소 카테고리. getMarketList 발견 시 채워지며, 거래 불가(수동 추가) 아이템은 기본 0을 유지한다.
    @Column(name = "main_category", nullable = false, columnDefinition = "integer default 0")
    var mainCategory: Int = 0,
    @Column(name = "sub_category", nullable = false, columnDefinition = "integer default 0")
    var subCategory: Int = 0,
    @Column(name = "tradeable", nullable = false)
    var tradeAble: Boolean,
    // 메타데이터 보강(재)수행이 필요한 행. 자동 감지/수동 시드 skeleton은 true로 삽입되고 보강 성공 시 false가 된다.
    @Column(name = "needs_update", nullable = false, columnDefinition = "boolean default false")
    var needsUpdate: Boolean = false,
    // 게임에서 삭제된 아이템. 사용자가 수동으로 관리하며 서빙에서 제외된다(가격 이력 보존을 위해 행은 유지).
    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    var deleted: Boolean = false,
    // 아이템 이미지를 GCP Storage에 업로드했는지 여부. 신규 행은 false로 시작하고 업로드 성공 시 true가 된다.
    @Column(name = "image_stored", nullable = false, columnDefinition = "boolean default false")
    var imageStored: Boolean = false,
) {
    fun toDTO() = BDOItemDTO(
        itemNum = itemNum,
        itemNameKr = itemNameKr,
        maxEnhancementLevel = maxEnhancementLevel,
        grade = grade,
        tradeAble = tradeAble,
        itemNameEn = itemNameEn,
    )
}
