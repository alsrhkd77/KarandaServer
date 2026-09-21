package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

@Tag(name = "Chzzk", description = "Chzzk live status API")
@RestController
@RequestMapping("/chzzk")
class ChzzkController {

    @GetMapping("/live-status")
    @Operation(summary = "Check whether the Karanda chzzk channel is broadcasting live")
    @SecurityRequirement(name = "Qualifications")
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
