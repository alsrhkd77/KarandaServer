package kr.karanda.karandaserver

import kr.karanda.karandaserver.controller.ChzzkController
import kr.karanda.karandaserver.util.TokenUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class ChzzkControllerTests {

    @Autowired
    private lateinit var tokenUtils: TokenUtils
    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToController(ChzzkController())
            .configureClient()
            .baseUrl("/chzzk")
            .defaultHeader("Qualification", tokenUtils.createQualificationToken())
            .build()
    }

    @Test
    fun `Assert Chzzk Api`() {
        client.get().uri("/live-status")
            .exchange()
            .expectStatus().isOk
    }

}