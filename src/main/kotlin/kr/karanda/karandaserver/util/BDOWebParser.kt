package kr.karanda.karandaserver.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class BDOWebParser {

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
        "34" to "deadeye"
    )

    /*
    fun parseProfile(profileCode: String, region: String = "KR"): ParsedFamilyProfile {
        val url =
            "https://www.kr.playblackdesert.com/ko-KR/Adventure/Profile?profileTarget=${profileCode}"
        val doc: Document = Jsoup.connect(url).get()
        val classType = doc.selectFirst("span.profile_img.icon_character")!!.classNames()
            .first { !it.equals("profile_img") && !it.equals("icon_character") }.replace("icn_character", "")
        val profileAreaElement: Element = doc.selectFirst("div.profile_info.no_profile")!!
        val lineList: Elements = doc.selectFirst("ul.line_list")!!.select("span.desc")     // 0:길드, 1:생성일, 2:공헌도
        val lifeSkillLevelArea = doc.selectFirst("div.character_spec")!!
        val characterLevels = doc.selectFirst("ul.character_list")!!.select("p.character_info")
            .map { it.child(1).select("em").text().toIntOrNull()?:0 }

        val familyName = profileAreaElement.selectFirst("p.nick")!!.text()
        val guild = lineList[0].text()
        val createdOn = lineList[1].text()
        val contributionPoints = lineList[2].text()

        val parsedData = ParsedFamilyProfile(
            region = profileAreaElement.selectFirst("span.region_info")?.text() ?: region,
            familyName = familyName,
            guild = guild,
            createdOn = createdOn,
            contributionPoints = contributionPoints,
            highestLevel = characterLevels.max(),
        )
        classNameMapping[classType]?.let { parsedData.mainClass = it }
        if (!lifeSkillLevelArea.classNames().contains("lock")) {
            val lifeSkillLevel = lifeSkillLevelArea.select("li")
                .map {
                    //it.selectFirst("span.icon_spec")!!.attr("data-tooltipbox") + "@" +
                    it.selectFirst("span.spec_level")!!.text()
                }
            parsedData.lifeSkillLevel = FamilyLifeSkillLevel(
                gathering = lifeSkillLevel[0],
                fishing = lifeSkillLevel[1],
                hunting = lifeSkillLevel[2],
                cooking = lifeSkillLevel[3],
                alchemy = lifeSkillLevel[4],
                processing = lifeSkillLevel[5],
                training = lifeSkillLevel[6],
                trade = lifeSkillLevel[7],
                farming = lifeSkillLevel[8],
                sailing = lifeSkillLevel[9],
                barter = lifeSkillLevel[10],
            )
        }
        return parsedData
    }
     */

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
}