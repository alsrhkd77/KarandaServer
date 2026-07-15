package kr.karanda.karandaserver.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * BDO 거래소 API 중 일부 엔드포인트(GetWorldMarketHotList / GetWorldMarketList /
 * GetBiddingInfoList)가 반환하는 Huffman 압축 바이너리를 원본 `resultMsg` 문자열로 복원한다.
 *
 * 나머지 엔드포인트는 여전히 `{resultCode, resultMsg}` JSON을 반환하므로 이 디코더가 필요 없다.
 *
 * [패킷 포맷 — 전체 little-endian]
 *   offset 0x00  int64  전체 응답 길이(자기 자신 포함)
 *   offset 0x08  int32  심볼 수(resultMsg 문자 집합 = 0-9, '-', '|' → 12)
 *   이후          심볼 수 × (int32 빈도, int32 문자코드)
 *   이후          int32  비트스트림 유효 비트 수
 *   이후          int32  비트스트림 바이트 수(= ceil(비트 수 / 8))
 *   이후          int32  디코딩 결과 문자 수(= 빈도 합)
 *   이후          비트스트림(MSB-first, 0=left, 1=right)
 *
 * Huffman 트리는 빈도표를 배열 기반 min-heap(비교는 빈도만, strict `<`,
 * push=sift-up / pop=swap_remove(0)+sift-down)에 넣고 두 개씩 pop(첫 번째=left,
 * 두 번째=right)→병합→push 하는 고전 방식으로 만든다. 동률(빈도가 같은 심볼)의 순서가
 * 이 힙의 구조적 동작으로 결정되므로 표준 [java.util.PriorityQueue] 등으로 대체하면
 * 다른 트리가 만들어져 디코딩이 깨진다.
 *
 * 참고 구현: https://github.com/Thell/bdo-huffman-rs
 */
object TradeMarketBinaryDecoder {

    private class Node(
        val frequency: Long,
        val symbol: Char? = null,
        val left: Node? = null,
        val right: Node? = null,
    )

    /**
     * 인코더(게임 서버)와 동률 처리까지 동일하게 동작해야 하는 배열 기반 min-heap.
     * 비교는 frequency만 사용하며(strict `<`), 동률 순서는 힙 구조에서 자연히 결정된다.
     */
    private class MinHeap {
        private val nodes = ArrayList<Node>()

        val size: Int get() = nodes.size

        fun push(node: Node) {
            nodes.add(node)
            var child = nodes.size - 1
            while (child > 0) {
                val parent = (child - 1) / 2
                if (nodes[child].frequency < nodes[parent].frequency) {
                    swap(child, parent)
                    child = parent
                } else break
            }
        }

        fun pop(): Node {
            val root = nodes[0]
            val last = nodes.removeAt(nodes.size - 1)
            if (nodes.isEmpty()) return root
            nodes[0] = last
            var parent = 0
            var child = 1
            val end = nodes.size
            while (child < end) {
                val right = child + 1
                if (right < end && nodes[child].frequency > nodes[right].frequency) {
                    child = right
                }
                if (nodes[child].frequency < nodes[parent].frequency) {
                    swap(parent, child)
                    parent = child
                    child = 2 * parent + 1
                } else break
            }
            return root
        }

        private fun swap(i: Int, j: Int) {
            val tmp = nodes[i]; nodes[i] = nodes[j]; nodes[j] = tmp
        }
    }

    /**
     * 압축 패킷을 원본 `resultMsg` 문자열로 디코딩한다.
     * 포맷이 예상과 다르면 예외를 던진다(추측 금지 — 호출부에서 처리).
     */
    fun decode(data: ByteArray): String {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val totalLength = buffer.long
        check(totalLength == data.size.toLong()) {
            "totalLength($totalLength) != data size(${data.size})"
        }

        val symbolCount = buffer.int
        val heap = MinHeap()
        repeat(symbolCount) {
            val frequency = buffer.int.toLong() and 0xFFFFFFFFL
            val symbol = buffer.int.toChar()
            heap.push(Node(frequency, symbol))
        }

        val bitLength = buffer.int
        val packedLength = buffer.int
        val decodedLength = buffer.int
        check(buffer.remaining() == packedLength) {
            "stream size mismatch: remaining=${buffer.remaining()}, packedLength=$packedLength"
        }

        while (heap.size > 1) {
            val left = heap.pop()
            val right = heap.pop()
            heap.push(Node(left.frequency + right.frequency, left = left, right = right))
        }
        val root = heap.pop()

        val result = StringBuilder(decodedLength)
        var node = root
        var consumed = 0
        outer@ for (byteIndex in 0 until packedLength) {
            val currentByte = buffer.get().toInt()
            for (bit in 7 downTo 0) {
                if (consumed >= bitLength) break@outer
                consumed++
                node = (if ((currentByte shr bit) and 1 == 0) node.left else node.right)
                    ?: error("invalid huffman stream")
                node.symbol?.let {
                    result.append(it)
                    node = root
                }
            }
        }
        check(result.length == decodedLength) {
            "decoded length mismatch: ${result.length} != $decodedLength"
        }
        return result.toString()
    }
}
