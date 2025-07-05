package kr.karanda.karandaserver.util

class RandomCodeUtils{

    private val numbers = ('1'..'9')
    private val lowerCase = ('a'..'z')
    private val upperCase = ('A'..'Z')

    fun generate(length: Int): String {
        val charset = numbers + upperCase
        return List(length) { charset.random() }.joinToString("")
    }
}