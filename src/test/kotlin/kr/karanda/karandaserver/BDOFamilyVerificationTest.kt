package kr.karanda.karandaserver

import kr.karanda.karandaserver.util.WebUtils
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [KarandaServerApplication::class])
class BDOFamilyVerificationTest {

    val profile =
        "https://www.kr.playblackdesert.com/ko-KR/Adventure/Profile?profileTarget=coSuKY1K7C%2f877ylTj8cZQGKK7UGfRgX3XDdZkjIJ7WICimi3Pk7gB%2bUh2ezrRPtUrsUw2%2fRU%2bTmmGk%2bF08sLuxbSf0c6%2fDswh0ABQECU5E495%2bZIQPWizY24bsrnmqX0TfxKGJIyNbC3hT5upauqq3zUty3IflKHFkjE9dI%2fson8zIFabBivVT59XZgtvI1"

    @Test
    fun `web parsing test`() {
        val parser = WebUtils()
        val data = parser.getAdventurerProfile(url = profile)
        println("가문명: ${data.familyName}")
        println("지역: ${data.region}")
        println("대표 클래스: ${data.mainClass}")
        println("생성일: ${data.createdOn}")
        println("길드: ${data.guild}")
        println("최대 공방합: ${data.maxGearScore ?: "비공개"}")
        println("기운: ${data.energy ?: "비공개"}")
        println("공헌도: ${data.contributionPoints ?: "비공개"}")
        println("캐릭터 레벨: ${data.characterLevels.joinToString()}")
        println("생활레벨 비공개: ${data.lifeSkillIsLocked}")
    }
}