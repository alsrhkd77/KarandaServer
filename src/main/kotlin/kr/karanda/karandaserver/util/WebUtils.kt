package kr.karanda.karandaserver.util

import kr.karanda.karandaserver.dto.BDOAdventurerProfile
import kr.karanda.karandaserver.dto.ParsedItem
import kr.karanda.karandaserver.enums.BDORegion
import kr.karanda.karandaserver.exception.ExternalApiException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WebUtils {

    private val classNameMapping = mapOf(
        "0" to "warrior",
        "4" to "ranger",
        "8" to "sorceress",
        "12" to "berserker",
        "16" to "tamer",
        "20" to "musa",
        "21" to "maehwa",
        "24" to "valkyrie",
        "25" to "kunoichi",
        "26" to "ninja",
        "28" to "wizard",
        "31" to "witch",
        "19" to "striker",
        "23" to "mystic",
        "11" to "lahn",
        "29" to "archer",
        "27" to "darkKnight",
        "17" to "shai",
        "5" to "guardian",
        "1" to "hashashin",
        "9" to "nova",
        "2" to "sage",
        "10" to "corsair",
        "7" to "drakania",
        "30" to "woosa",
        "15" to "maegu",
        "6" to "scholar",
        "33" to "dosa",
        "34" to "deadeye",
        "3" to "wukong"
    )

    fun getAdventurerProfile(url: String): BDOAdventurerProfile {
        try {
            val doc: Document = Jsoup.connect(url).get()
            val profileDetail = doc.selectFirst("div.profile_detail") ?: throw ExternalApiException()
            val familyName = profileDetail.selectFirst("p.nick")!!.text()
            val region = profileDetail.selectFirst("span.region_info")?.text()
            val classType = doc.selectFirst("span.profile_img.icon_character")!!.classNames()
                .first { !it.equals("profile_img") && !it.equals("icon_character") }.replace("icn_character", "")

            val lineList: List<Element> = doc.selectFirst("ul.line_list")!!.children().map {
                it.selectFirst("span.desc") ?: throw ExternalApiException()
            }     // 0:생성일, 1:길드, 2:최대 공방합, 3:기운, 4:공헌도
            val createdOn = lineList[0].text()
            val guild = lineList[1].let {
                if (it.selectFirst("em.lock") != null) {
                    null
                } else if (it.selectFirst("span.desc")?.selectFirst("span.desc") != null) {
                    ""
                } else {
                    it.selectFirst("span.desc")?.text() ?: ""
                }
            }
            val maxGearScore = lineList[2].text().toIntOrNull()
            val energy = lineList[3].text().toIntOrNull()
            val contributionPoints = lineList[4].text().toIntOrNull()

            val characterLevels = doc.selectFirst("ul.character_list")!!.select("p.character_info")
                .map { it.child(1).select("em").text().toIntOrNull() ?: 0 }

            val lifeSkillIsLocked = doc.selectFirst("div.character_data_box.lock") != null

            return BDOAdventurerProfile(
                familyName = familyName,
                region = region?.let {
                    try {
                        BDORegion.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                },
                mainClass = classNameMapping[classType] ?: "unknown",
                createdOn = createdOn,
                guild = guild,
                maxGearScore = maxGearScore,
                energy = energy,
                contributionPoints = contributionPoints,
                characterLevels = characterLevels,
                lifeSkillIsLocked = lifeSkillIsLocked,
            )
        } catch (e: Exception) {
            println(e)
            throw ExternalApiException()
        }
    }

    fun getItemName(url: String, original: String): String {
        val doc: Document = Jsoup.connect(url).get()
        val main = doc.selectFirst("div.item_title > b")?.text() ?: throw Exception("Failed to parse main title")
        val sub =
            doc.selectFirst("span.item_sub_title > b")?.text() ?: throw Exception("Failed to parse sub title")
        if (main.trim().equals(original, ignoreCase = true)) {
            return sub
        } else {
            throw Exception("Item name is not the same!")
        }
    }

    /**
     * 아이템 상세페이지에서 한글명·영문명·등급을 파싱한다.
     *
     * 신규 아이템은 한글명 자체를 페이지에서 얻어야 하므로 [getItemName]의 이름 일치 검증을 쓰지 않는다.
     * 등급은 `div.item_title`의 `item_grade_N` 클래스에서 N을 추출한다.
     * 하나라도 파싱에 실패하면 예외를 던진다(추측 금지 — 호출부에서 로그 후 재시도).
     */
    fun getItemMetadata(url: String): ParsedItem {
        val doc: Document = Jsoup.connect(url).get()
        val titleElement = doc.selectFirst("div.item_title") ?: throw Exception("Failed to parse item title")
        val nameKr = titleElement.selectFirst("b")?.text()?.trim() ?: throw Exception("Failed to parse main title")
        val nameEn =
            doc.selectFirst("span.item_sub_title > b")?.text()?.trim() ?: throw Exception("Failed to parse sub title")
        val grade = titleElement.classNames()
            .firstOrNull { it.startsWith("item_grade_") }
            ?.removePrefix("item_grade_")
            ?.toIntOrNull()
            ?: throw Exception("Failed to parse item grade")
        return ParsedItem(nameKr = nameKr, nameEn = nameEn, grade = grade)
    }
}