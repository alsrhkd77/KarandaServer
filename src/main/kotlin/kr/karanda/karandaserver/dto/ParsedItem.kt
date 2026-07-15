package kr.karanda.karandaserver.dto

/**
 * 아이템 상세페이지(`parse` URL)에서 파싱한 메타데이터.
 */
data class ParsedItem(
    val nameKr: String,
    val nameEn: String,
    val grade: Int,
)
