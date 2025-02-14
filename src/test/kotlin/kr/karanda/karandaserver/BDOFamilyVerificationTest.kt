package kr.karanda.karandaserver

import kr.karanda.karandaserver.util.BDOWebParser
import kr.karanda.karandaserver.util.difference
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.TimeSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [KarandaServerApplication::class])
class BDOFamilyVerificationTest {

    val codeList = listOf(
        "coSuKY1K7C%2f877ylTj8cZQGKK7UGfRgX3XDdZkjIJ7WICimi3Pk7gB%2bUh2ezrRPtUrsUw2%2fRU%2bTmmGk%2bF08sLuxbSf0c6%2fDswh0ABQECU5E495%2bZIQPWizY24bsrnmqX0TfxKGJIyNbC3hT5upauqq3zUty3IflKHFkjE9dI%2fson8zIFabBivVT59XZgtvI1",
        //"tbXSK7e39Sb3U3yPi7UDjuBG6ABbRlAyaVHYU1lVpi77E5YODejB%2fXXmItw6xegMxfLfcXcyqXVoQvlEpSC709ex%2bFwunM%2fQHdAEgsbvuCb%2b38ZJKrw%2fGUzVOC4oAI2ywenXrUPd1FoL0szlYv4U4mc63Rdabtrrhj1UoK28P5w%3d",
        //"coSuKY1K7C%2f877ylTj8cZXa3JJ%2f%2ffb%2bzHIDIP9A5ZXQADXF7ZMMblrBd5wRtWShMwIUvAUpTgmTtGraJGEpFe%2frII2BgNH5XYfUzvveqkA2qPHipSB6IeApdztVFAH0g7R%2b%2fW49BDeoBwthpwWTPhQiycPAC7%2fxLD2D13b5UPOQ%3d"
    )

    @Test
    fun `web parsing test`() {
        val parser = BDOWebParser()
        val data = parser.parseProfile(profileCode = codeList.random(Random(System.currentTimeMillis())), region = "KR")
        println(data.familyName)
        println(data.guild)
        println(data.lifeSkillLevel)
        println(data.createdOn)
        println(data.mainClass)
        println(data.contributionPoints)
        println(data.verificationOn)
        println(data.highestLevel)
    }
}