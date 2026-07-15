package kr.karanda.karandaserver

import kr.karanda.karandaserver.util.SingleFlightExecutor
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [SingleFlightExecutor] 단위 검증 — Spring 컨텍스트 불필요.
 */
class SingleFlightExecutorTest {

    @Test
    fun `concurrent calls with same key run supplier once`() {
        val singleFlight = SingleFlightExecutor()
        val calls = AtomicInteger(0)
        val threads = 32
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)

        val futures = (1..threads).map {
            pool.submit<String> {
                ready.countDown()
                start.await()
                singleFlight.execute("same-key") {
                    calls.incrementAndGet()
                    Thread.sleep(50)  // 리더가 실행 중인 동안 팔로워가 합류하도록 창을 연다
                    "result"
                }
            }
        }
        ready.await()
        start.countDown()

        val results = futures.map { it.get() }
        pool.shutdown()

        assertTrue(results.all { it == "result" }, "all callers must share the result")
        assertEquals(1, calls.get(), "supplier must run exactly once for concurrent same-key calls")
    }

    @Test
    fun `different keys run independently`() {
        val singleFlight = SingleFlightExecutor()
        val calls = AtomicInteger(0)
        singleFlight.execute("a") { calls.incrementAndGet() }
        singleFlight.execute("b") { calls.incrementAndGet() }
        assertEquals(2, calls.get())
    }

    @Test
    fun `leader failure propagates original exception and key is released`() {
        val singleFlight = SingleFlightExecutor()
        assertFailsWith<IllegalStateException> {
            singleFlight.execute("k") { throw IllegalStateException("boom") }
        }
        // 실패 후 키가 해제되어 다음 요청이 새 리더로 정상 실행되는지 확인
        val result = singleFlight.execute("k") { "recovered" }
        assertEquals("recovered", result)
    }
}
