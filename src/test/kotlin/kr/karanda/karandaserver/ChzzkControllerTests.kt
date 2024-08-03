package kr.karanda.karandaserver

import kr.karanda.karandaserver.controller.ChzzkController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class ChzzkControllerTests {

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToController(ChzzkController())
            .configureClient()
            .baseUrl("/chzzk")
            .build()
    }

    @Test
    fun `Assert Chzzk Api`() {
        client.get().uri("/live-status")
            .exchange()
            .expectStatus().isOk
    }

}