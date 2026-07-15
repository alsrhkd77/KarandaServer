package kr.karanda.karandaserver.dto

data class BDOItem(
    val itemNum: Int,
    val itemNameKr: String?,
    val itemNameEn: String?,
    val maxEnhancementLevel: Int?,
    val grade: Int?,
    val tradeAble: Boolean,
)
