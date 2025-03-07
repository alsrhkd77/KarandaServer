package kr.karanda.karandaserver.util

import java.security.SecureRandom
import java.util.*

/**
 * Custom UUID 생성을 위한 클래스.
 */
class UUIDUtils {

    /**
     * Custom된 **UUID v1**을 생성.
     *
     * **UUID v1**과 같지만 MAC주소 대신 랜덤값 사용.
     *
     * @return [UUID]
     */
    fun generateUUID1(): UUID {
        val most64SigBits = get64MostSignificantBitsForVersion1()
        val least64SigBits = get64LeastSignificantBitsForVersion1()
        return UUID(most64SigBits, least64SigBits)
    }

    private fun get64MostSignificantBitsForVersion1(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val timeLow = (currentTimeMillis and 0x00000000FFFFFFFFL) shl 32
        val timeMid = ((currentTimeMillis shr 32) and 0xFFFFL) shl 16
        val version = (1 shl 12).toLong()
        val timeHi = ((currentTimeMillis shr 48) and 0x0FFFL)
        return timeLow or timeMid or version or timeHi
    }

    private fun get64LeastSignificantBitsForVersion1(): Long {
        val random = SecureRandom()
        val random63BitLong: Long = random.nextLong() and 0x3FFFL
        val variant3BitFlag = (0x8000L).toLong()
        return random63BitLong or variant3BitFlag
    }
}