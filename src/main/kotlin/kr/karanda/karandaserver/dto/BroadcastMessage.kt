package kr.karanda.karandaserver.dto

data class BroadcastMessage(
    var destinations: MutableList<String> = mutableListOf(),
    var message: String = ""
)
