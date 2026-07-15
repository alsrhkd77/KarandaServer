package kr.karanda.karandaserver.util

/**
 * 거래소 카테고리 분류(mainCategory / subCategory).
 *
 * 거래소 웹 페이지 categoryList의 `li[data-main][data-sub]`에서 추출해 하드코딩했다.
 * hot(급등락)·wait(등록 대기)은 실제 아이템 카테고리가 아니므로 제외한다.
 * 일부 subCategory 번호는 비어 있어(예: main=10의 18, main=65의 8) 연속 범위가 아니라 실제 존재하는 번호만 담는다.
 *
 * 게임 업데이트로 신규 클래스 무기/카테고리가 추가되면 이 목록을 갱신해야 한다.
 */
object BdoMarketCategory {
    // main 순서는 웹 categoryList 표기 순서를 따른다.
    private val subCategoriesByMain: Map<Int, List<Int>> = linkedMapOf(
        1 to (1..25).toList(),                 // 주 무기
        5 to (1..23).toList(),                 // 보조 무기
        10 to ((1..17) + (19..31)),            // 각성 무기 (18 없음)
        15 to (1..6).toList(),                 // 방어구
        20 to (1..4).toList(),                 // 액세서리
        85 to (1..6).toList(),                 // 유물/광명석
        25 to (1..11).toList(),                // 재료
        30 to (1..3).toList(),                 // 강화 관련
        35 to (1..9).toList(),                 // 소모품
        40 to (1..10).toList(),                // 생활 도구
        45 to (1..4).toList(),                 // 연금석
        50 to (1..4).toList(),                 // 마력의 수정
        55 to (1..8).toList(),                 // 펄 상품
        60 to (1..9).toList(),                 // 염색약
        65 to ((1..7) + (9..13)),              // 동물 (8 없음)
        70 to (1..9).toList(),                 // 선박
        75 to (1..6).toList(),                 // 마차
        80 to (1..9).toList(),                 // 가구
    )

    /** 순회 대상 (mainCategory, subCategory) 페어 전체. */
    val pairs: List<Pair<Int, Int>> = subCategoriesByMain.entries.flatMap { (main, subs) ->
        subs.map { main to it }
    }
}
