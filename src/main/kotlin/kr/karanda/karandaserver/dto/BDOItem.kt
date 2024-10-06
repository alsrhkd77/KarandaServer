package kr.karanda.karandaserver.dto

data class BDOItem(
    val itemNum: Int,
    val itemNameKr: String,
    val maxEnhancementLevel: Int,
    val grade: Int,
    val categoryNum: String,
    val categoryNameKr: String,
    val tradeAble: Boolean,
)
