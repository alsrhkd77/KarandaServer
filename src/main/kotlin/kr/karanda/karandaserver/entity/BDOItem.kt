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
    var itemNameKr: String,
    var itemNameEn: String?,
    var maxEnhancementLevel: Int,
    var grade: Int,
    var categoryNum: String,
    var categoryNameKr: String,
    @Column(name = "tradeable", nullable = false)
    var tradeAble: Boolean,
) {
    fun toDTO() = BDOItemDTO(
        itemNum = itemNum,
        itemNameKr = itemNameKr,
        maxEnhancementLevel = maxEnhancementLevel,
        grade = grade,
        categoryNum = categoryNum,
        categoryNameKr = categoryNameKr,
        tradeAble = tradeAble,
        itemNameEn = itemNameEn,
    )
}