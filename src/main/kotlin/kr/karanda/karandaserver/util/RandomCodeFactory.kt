package kr.karanda.karandaserver.util

class RandomCodeFactory{

    private val numbers = ('1'..'9')
    private val lowerCase = ('a'..'z')
    private val upperCase = ('A'..'Z')

    fun generate(length: Int): String {
        val charset = numbers + lowerCase
        return List(length) { charset.random() }.joinToString("")
    }
}