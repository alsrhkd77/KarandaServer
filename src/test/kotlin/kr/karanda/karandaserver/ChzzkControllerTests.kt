package kr.karanda.karandaserver

import kr.karanda.karandaserver.controller.ChzzkController
import kr.karanda.karandaserver.util.TokenFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
class ChzzkControllerTests {

    @Autowired
    private lateinit var tokenFactory: TokenFactory
    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToController(ChzzkController())
            .configureClient()
            .baseUrl("/chzzk")
            .defaultHeader("Qualification", tokenFactory.createQualificationToken())
            .build()
    }

    @Test
    fun `Assert Chzzk Api`() {
        client.get().uri("/live-status")
            .exchange()
            .expectStatus().isOk
    }

}