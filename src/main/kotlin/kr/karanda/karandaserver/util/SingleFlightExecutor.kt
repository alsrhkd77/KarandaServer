package kr.karanda.karandaserver.util

import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-flight 실행기.
 *
 * 같은 `key`에 대한 동시 요청 중 하나(리더)만 [execute]의 supplier를 실행하고,
 * 나머지(팔로워)는 리더의 결과를 공유한다. 동일 대상에 대한 중복 외부 호출을 제거하는 데 사용한다.
 *
 * 리더가 예외로 끝나면 그 시점의 팔로워들도 동일 예외를 공유한다(즉시 재시도로 인한 외부 API 폭주 방지).
 * 실행이 끝나면 항목이 제거되므로, 이후 도착하는 요청은 새 리더로 다시 실행된다.
 */
@Component
class SingleFlightExecutor {

    private val inFlight = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    fun <T> execute(key: String, supplier: () -> T): T {
        val future = CompletableFuture<Any?>()
        val existing = inFlight.putIfAbsent(key, future)
        if (existing != null) return awaitResult(existing)

        try {
            val result = supplier()
            future.complete(result)
            return result
        } catch (e: Throwable) {
            future.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, future)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> awaitResult(future: CompletableFuture<Any?>): T {
        try {
            return future.join() as T
        } catch (e: CompletionException) {
            // 원래 예외(@ResponseStatus 등)를 그대로 전파하도록 언랩한다.
            throw e.cause ?: e
        }
    }
}
