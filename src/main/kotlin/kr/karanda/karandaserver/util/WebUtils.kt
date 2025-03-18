package kr.karanda.karandaserver.util

import kr.karanda.karandaserver.dto.BDOAdventurerProfile
import kr.karanda.karandaserver.dto.BDOLifeSkillLevels
import kr.karanda.karandaserver.exception.ExternalApiException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

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
        "34" to "deadeye"
    )

    fun getAdventurerProfile(url: String): BDOAdventurerProfile {
        try {
            val doc: Document = Jsoup.connect(url).get()
            val lineList: Elements = doc.selectFirst("ul.line_list")!!.select("span.desc")     // 0:길드, 1:생성일, 2:공헌도

            val guild = lineList[0].text()
            val createdOn = lineList[1].text()
            val contributionPoints = lineList[2].text().toIntOrNull()
            val familyName = doc.selectFirst("div.profile_info.no_profile")!!.selectFirst("p.nick")!!.text()
            val classType = doc.selectFirst("span.profile_img.icon_character")!!.classNames()
                .first { !it.equals("profile_img") && !it.equals("icon_character") }.replace("icn_character", "")
            val characterLevels = doc.selectFirst("ul.character_list")!!.select("p.character_info")
                .map { it.child(1).select("em").text().toIntOrNull() ?: 0 }
            val lifeSkillLevel = doc.selectFirst("div.character_spec")?.select("li")
                ?.map { it.selectFirst("span.spec_level")?.text() ?: "?" }
                ?.let { BDOLifeSkillLevels(it) }

            return BDOAdventurerProfile(
                familyName = familyName,
                mainClass = classNameMapping[classType] ?: "unknown",
                createdOn = createdOn,
                guild = guild,
                contributionPoints = contributionPoints,
                charactersLevel = characterLevels,
                lifeSkills = lifeSkillLevel
            )
        } catch (e: Exception) {
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
}