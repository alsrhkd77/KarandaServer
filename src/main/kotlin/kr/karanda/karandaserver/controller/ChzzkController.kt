package kr.karanda.karandaserver.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

@RestController
@RequestMapping("/chzzk")
class ChzzkController {

    @GetMapping("/live-status")
    fun getLiveStatus(): Boolean {
        val id = "e28fd3efe38595427f8e51142c91b247"
        val client = RestClient.create("https://api.chzzk.naver.com")
        val result = client.get()
            .uri("/service/v1/channels/{id}", id)
            .header("User-Agent", "Mozilla/5.0")
            .retrieve()
            .body(Chzzk::class.java)
        return (result?.content?.get("openLive") ?: false) as Boolean
    }

}

private data class Chzzk(val code: Int, val message: String?, val content: Map<String, Any>)
