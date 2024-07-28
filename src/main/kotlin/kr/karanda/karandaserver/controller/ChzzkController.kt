package kr.karanda.karandaserver.controller

import kr.karanda.karandaserver.data.Chzzk
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient

@RestController
@RequestMapping("/chzzk")
class ChzzkController {

    @GetMapping("/live-status")
    fun getLiveStatus(): Boolean {
        val id = "e28fd3efe38595427f8e51142c91b247"
        val client = WebClient.create("https://api.chzzk.naver.com")
        val result = client.get()
            .uri("/service/v1/channels/{id}", id)
            .header("User-Agent", "Mozilla/5.0")
            .retrieve()
            .bodyToMono(Chzzk::class.java)
            .block()
        return (result?.content?.get("openLive") ?: false) as Boolean
    }

}