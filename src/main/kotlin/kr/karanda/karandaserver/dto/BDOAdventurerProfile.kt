package kr.karanda.karandaserver.dto

import kr.karanda.karandaserver.enums.BDORegion

data class BDOAdventurerProfile(
    var familyName: String,
    var region: BDORegion?,         //
    var mainClass: String,
    var createdOn: String,
    var guild: String?,             //"": 길드 없음, null: 비공개
    var maxGearScore: Int?,         //최대 공방합
    var energy: Int?,               //기운
    var contributionPoints: Int?,   //공헌도
    var characterLevels: List<Int>,
    var lifeSkillIsLocked: Boolean, //생활 레벨 비공개 여부
)