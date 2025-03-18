package kr.karanda.karandaserver.dto

data class BDOAdventurerProfile(
    var familyName: String,
    var mainClass: String,
    var createdOn: String,
    var guild: String,
    var contributionPoints: Int?,
    var charactersLevel: List<Int>,
    var lifeSkills: BDOLifeSkillLevels?,
)