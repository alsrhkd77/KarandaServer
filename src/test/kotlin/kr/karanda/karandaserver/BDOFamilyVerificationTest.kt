package kr.karanda.karandaserver

import kr.karanda.karandaserver.util.BDOWebParser
import kr.karanda.karandaserver.util.difference
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [KarandaServerApplication::class])
class BDOFamilyVerificationTest {

    val codeList = listOf(
        "coSuKY1K7C%2f877ylTj8cZQGKK7UGfRgX3XDdZkjIJ7WICimi3Pk7gB%2bUh2ezrRPtUrsUw2%2fRU%2bTmmGk%2bF08sLuxbSf0c6%2fDswh0ABQECU5E495%2bZIQPWizY24bsrnmqX0TfxKGJIyNbC3hT5upauqq3zUty3IflKHFkjE9dI%2fson8zIFabBivVT59XZgtvI1",
        "tbXSK7e39Sb3U3yPi7UDjuBG6ABbRlAyaVHYU1lVpi77E5YODejB%2fXXmItw6xegMxfLfcXcyqXVoQvlEpSC709ex%2bFwunM%2fQHdAEgsbvuCb%2b38ZJKrw%2fGUzVOC4oAI2ywenXrUPd1FoL0szlYv4U4mc63Rdabtrrhj1UoK28P5w%3d"
    )

    @Test
    fun `web parsing test`() {
        val parser = BDOWebParser()
        val data = parser.parseProfile(profileCode = codeList.random(), region = "KR")
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val pre = now.minusHours(12)
        println(pre.difference(now))
        println(data.familyName)
        println(data.createdOn)
        println(data.mainClass)
        println(data.contributionPoints)
        println(data.verificationOn)
    }
}