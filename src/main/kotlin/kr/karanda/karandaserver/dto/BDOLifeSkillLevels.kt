package kr.karanda.karandaserver.dto

data class BDOLifeSkillLevels(
    var gathering: String,
    var fishing: String,
    var hunting: String,
    var cooking: String,
    var alchemy: String,
    var processing: String,
    var training: String,
    var trade: String,
    var farming: String,
    var sailing: String,
    var barter: String,
) {
    constructor(data: List<String>) : this(
        gathering = data[0],
        fishing = data[1],
        hunting = data[2],
        cooking = data[3],
        alchemy = data[4],
        processing = data[5],
        training = data[6],
        trade = data[7],
        farming = data[8],
        sailing = data[9],
        barter = data[10],
    )
}