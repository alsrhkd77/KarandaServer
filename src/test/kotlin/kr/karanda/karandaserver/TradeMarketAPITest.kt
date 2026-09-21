package kr.karanda.karandaserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.karanda.karandaserver.util.TradeMarketBinaryDecoder
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * BDO 거래소 API(https://developers.veliainn.com) 탐색용 테스트.
 *
 * Firestore 설정에 의존하지 않도록 kr 리전 base URL과 헤더를 직접 지정해
 * 각 엔드포인트를 실제로 호출하고 응답 구조를 확인한다.
 *
 * 외부 라이브 API를 호출하므로 네트워크가 필요하며, 일반 단위 테스트와 분리해
 * 필요할 때만 수동으로 실행하는 것을 권장한다.
 *
 * [확인된 현재 동작 — 2026-07-05 기준]
 * 문서(HTML)에는 7개 엔드포인트 모두 `{resultCode, resultMsg}` JSON을 반환한다고
 * 되어 있으나, 실제로는 다음과 같이 나뉜다.
 *
 * JSON 반환 (Content-Type: application/json):
 *   - GetWorldMarketSubList
 *   - GetWorldMarketSearchList
 *   - GetMarketPriceInfo
 *   - GetWorldMarketWaitList
 *
 * Huffman 압축 바이너리 반환 (Content-Type: application/octet-stream):
 *   - GetWorldMarketHotList
 *   - GetWorldMarketList
 *   - GetBiddingInfoList
 *
 * [바이너리 포맷 — 리버스 엔지니어링으로 확정, 라이브 데이터 교차 검증 완료]
 * 전체 little-endian.
 *   offset 0x00  int64  totalLen        (자기 자신을 포함한 전체 응답 길이)
 *   offset 0x08  int32  symbolCount     (심볼 수; resultMsg 문자 집합 = 0-9, '-', '|' → 12)
 *   이후 symbolCount × (int32 빈도, int32 문자코드) 쌍
 *   이후 int32  bitLength   (비트스트림 유효 비트 수)
 *   이후 int32  packedLength(비트스트림 바이트 수 = ceil(bitLength/8))
 *   이후 int32  decodedLength(디코딩 결과 문자 수 = 빈도 합)
 *   이후 packedLength 바이트의 Huffman 비트스트림 (MSB-first, 0=left, 1=right)
 *
 * Huffman 트리는 빈도표를 배열 기반 min-heap(비교: 빈도만, strict <,
 * push=sift-up / pop=swap_remove(0)+sift-down)에 넣고 두 개씩 pop(첫 번째=left,
 * 두 번째=right)→병합→push 하는 고전 방식으로 만든다. 동률(빈도가 같은 심볼)의
 * 순서가 이 힙의 구조적 동작으로 결정되므로 반드시 동일하게 재현해야 한다.
 * (참고 구현: https://github.com/Thell/bdo-huffman-rs)
 * 디코딩 결과는 JSON 엔드포인트와 동일한 resultMsg 문자열이다.
 *
 * 검증: 12개 카테고리 GetWorldMarketList 615항목 전부를
 * GetWorldMarketSearchList(JSON) 실데이터와 대조해 완전 일치 확인.
 */
@Tag("external")
class TradeMarketAPITest {

    // kr 리전 거래소 base URL (HTML 문서 기준)
    private val baseUrl = "https://trade.kr.playblackdesert.com/Trademarket"

    private val client: RestClient = RestClient.builder()
        .defaultHeader("Content-Type", "application/json")
        .defaultHeader("User-Agent", "BlackDesert")
        .build()

    private val objectMapper = jacksonObjectMapper()

    data class TradeMarketResponse(
        val resultCode: Int = -1,
        val resultMsg: String = "",
    )

    /**
     * JSON을 반환하는 엔드포인트를 호출해 파싱한다.
     * 응답을 String으로 받은 뒤 직접 파싱해 Content-Type 협상 문제를 피한다.
     */
    private fun postJson(endpoint: String, payload: Map<String, Any>): TradeMarketResponse {
        val raw = client.post()
            .uri("$baseUrl/$endpoint")
            .body(payload)
            .retrieve()
            .body(String::class.java)!!
        return objectMapper.readValue(raw, TradeMarketResponse::class.java)
    }

    /**
     * Huffman 압축 바이너리를 반환하는 엔드포인트를 호출해 resultMsg 문자열로 디코딩한다.
     * 디코딩 로직은 프로덕션 코드([TradeMarketBinaryDecoder])를 그대로 사용한다.
     */
    private fun postBinary(endpoint: String, payload: Map<String, Any>): String {
        val bytes = client.post()
            .uri("$baseUrl/$endpoint")
            .body(payload)
            .retrieve()
            .body(ByteArray::class.java)!!
        return TradeMarketBinaryDecoder.decode(bytes)
    }

    // ---------------------------------------------------------------------
    // JSON 반환 엔드포인트
    // ---------------------------------------------------------------------

    @Test
    fun getWorldMarketSubList() {
        // 특정 아이템 강화단계별 상세. mainKey=10007.
        // 응답 구조: id-강화min-강화max-기준가-재고-누적거래-하한가-상한가-최근거래가-최근거래시각
        val response = postJson(
            "GetWorldMarketSubList",
            mapOf(
                "keyType" to 0,
                "mainKey" to 10007,
            )
        )
        println("[GetWorldMarketSubList] resultCode=${response.resultCode}")
        println("[GetWorldMarketSubList] resultMsg=${response.resultMsg}")

        assertEquals(0, response.resultCode)
        assertTrue(response.resultMsg.isNotBlank())
        assertTrue(response.resultMsg.split("|").filter { it.isNotEmpty() }
            .all { it.split("-").size == 10 })
    }

    @Test
    fun getWorldMarketSearchList() {
        // 아이템 ID 목록으로 검색. searchResult="10210,10286".
        // 응답 구조: id-재고-기준가-누적거래
        val response = postJson(
            "GetWorldMarketSearchList",
            mapOf(
                "searchResult" to "10210,10286",
            )
        )
        println("[GetWorldMarketSearchList] resultCode=${response.resultCode}")
        println("[GetWorldMarketSearchList] resultMsg=${response.resultMsg}")

        assertEquals(0, response.resultCode)
        assertTrue(response.resultMsg.isNotBlank())
        assertTrue(response.resultMsg.split("|").filter { it.isNotEmpty() }
            .all { it.split("-").size == 4 })
    }

    @Test
    fun getMarketPriceInfo() {
        // 최근 90일 가격 히스토리. mainKey=10210, subKey=0.
        val response = postJson(
            "GetMarketPriceInfo",
            mapOf(
                "keyType" to 0,
                "mainKey" to 10210,
                "subKey" to 0,
            )
        )
        println("[GetMarketPriceInfo] resultCode=${response.resultCode}")
        println("[GetMarketPriceInfo] resultMsg=${response.resultMsg}")

        assertEquals(0, response.resultCode)
        assertTrue(response.resultMsg.isNotBlank())
        assertTrue(response.resultMsg.split("-").all { it.toLongOrNull() != null })
    }

    @Test
    fun getWorldMarketWaitList() {
        // 등록 대기 중인 아이템 목록. 파라미터 없음.
        // 응답 구조: id-강화단계-가격-등록완료시각(epoch seconds)
        val response = postJson("GetWorldMarketWaitList", emptyMap())
        println("[GetWorldMarketWaitList] resultCode=${response.resultCode}")
        println("[GetWorldMarketWaitList] resultMsg=${response.resultMsg}")

        assertEquals(0, response.resultCode)
        // 대기 목록은 비어 있을 수 있으므로 resultCode만 검증한다.
    }

    // ---------------------------------------------------------------------
    // Huffman 압축 바이너리 반환 엔드포인트
    // ---------------------------------------------------------------------

    @Test
    fun getWorldMarketHotList() {
        // 핫 아이템 목록. 파라미터 없음.
        // 응답 구조: id-강화min-강화max-기준가-재고-누적거래-등락방향(1↓/2↑)-등락폭
        //           -하한가-상한가-최근거래가-최근거래시각 (12필드)
        val decoded = postBinary("GetWorldMarketHotList", emptyMap())
        val entries = decoded.split("|").filter { it.isNotEmpty() }
        println("[GetWorldMarketHotList] ${entries.size} entries")
        println("[GetWorldMarketHotList] first=${entries.firstOrNull()}")

        assertTrue(entries.isNotEmpty())
        assertTrue(entries.all { it.split("-").size == 12 })
    }

    @Test
    fun getWorldMarketList() {
        // 카테고리/서브카테고리별 아이템 목록. mainCategory=1(주무기), subCategory=1(장검).
        // 응답 구조: id-재고-누적거래-기준가 (4필드)
        val decoded = postBinary(
            "GetWorldMarketList",
            mapOf(
                "keyType" to 0,
                "mainCategory" to 1,
                "subCategory" to 1,
            )
        )
        println("[GetWorldMarketList] decoded=$decoded")
        val entries = decoded.split("|").filter { it.isNotEmpty() }.map { it.split("-") }

        assertTrue(entries.isNotEmpty())
        assertTrue(entries.all { it.size == 4 })

        // 교차 검증: 디코딩된 아이템들을 JSON 엔드포인트(GetWorldMarketSearchList)로
        // 조회해 재고/기준가/누적거래가 일치하는지 확인한다.
        // (search 구조: id-재고-기준가-누적거래 — 필드 순서가 다름에 주의)
        val ids = entries.map { it[0] }
        val search = postJson("GetWorldMarketSearchList", mapOf("searchResult" to ids.joinToString(",")))
        assertEquals(0, search.resultCode)
        val truth = search.resultMsg.split("|").filter { it.isNotEmpty() }
            .map { it.split("-") }
            .associateBy { it[0] }
        for (entry in entries) {
            val expected = truth[entry[0]]
            // 대기 등록 중 아이템은 search에 없을 수 있음
            if (expected != null) {
                assertEquals(expected[1], entry[1], "stock mismatch for item ${entry[0]}")
                assertEquals(expected[2], entry[3], "price mismatch for item ${entry[0]}")
                assertEquals(expected[3], entry[2], "trades mismatch for item ${entry[0]}")
            }
        }
        println("[GetWorldMarketList] cross-check vs SearchList: ${entries.count { truth.containsKey(it[0]) }}/${entries.size} verified")
    }

    @Test
    fun crossCheckSellOrdersEqualsStock() {
        // "판매 대기 수량 = 재고(SubList의 재고 필드)" 가정 검증.
        // 강화 단계별로 BiddingInfoList의 판매 주문 수량 합과 SubList의 재고를 대조한다.
        // 재고 0끼리 비교하면 무의미하므로, 재고>0인 (아이템, 단계)만 유효 검증으로 집계한다.
        // 유동성 높은 후보를 여러 개 두어 재고>0 표본을 확보한다.
        val candidateItems = listOf(721003, 16001, 16002, 4997, 4998, 10210)
        var verifiedNonZero = 0
        for (mainKey in candidateItems) {
            val subList = postJson(
                "GetWorldMarketSubList",
                mapOf(
                    "keyType" to 0,
                    "mainKey" to mainKey,
                )
            )
            if (subList.resultCode != 0) continue
            val levels = subList.resultMsg.split("|").filter { it.isNotEmpty() }.map { it.split("-") }
            for (level in levels) {
                if (level.size < 10) continue  // SubList 정상 행은 10필드. 예외 형식은 건너뜀
                val subKey = level[1]
                val stock = level[4].toLong()
                val decoded = postBinary(
                    "GetBiddingInfoList",
                    mapOf(
                        "keyType" to 0,
                        "mainKey" to mainKey,
                        "subKey" to subKey,
                    )
                )
                val sellOrders = decoded.split("|").filter { it.isNotEmpty() }
                    .map { it.split("-") }
                    .filter { it.size == 3 }  // BiddingInfo 정상 행은 3필드(가격-판매-구매)
                    .sumOf { it[1].toLong() }
                println("[crossCheck] item=$mainKey subKey=$subKey stock=$stock sumSellOrders=$sellOrders")
                assertEquals(stock, sellOrders, "sell orders != stock for item=$mainKey subKey=$subKey")
                if (stock > 0) verifiedNonZero++
            }
        }
        assertTrue(verifiedNonZero > 0, "재고>0 표본이 없어 가정 검증이 무의미함 — 유동성 높은 아이템으로 재시도 필요")
        println("[crossCheck] 재고>0 유효 검증 표본 수 = $verifiedNonZero")
    }

    @Test
    fun getBiddingInfoList() {
        // 특정 아이템의 매수/매도 주문 정보. mainKey=10210, subKey=0.
        // 응답 구조: 가격-판매주문수-구매주문수 (3필드)
        val decoded = postBinary(
            "GetBiddingInfoList",
            mapOf(
                "keyType" to 0,
                "mainKey" to 10210,
                "subKey" to 0,
            )
        )
        println("[GetBiddingInfoList] decoded=$decoded")
        val entries = decoded.split("|").filter { it.isNotEmpty() }

        assertTrue(entries.isNotEmpty())
        assertTrue(entries.all { it.split("-").size == 3 })
    }
}
