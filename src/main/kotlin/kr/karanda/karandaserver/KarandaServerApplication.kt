package kr.karanda.karandaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KarandaServerApplication

fun main(args: Array<String>) {

    runApplication<KarandaServerApplication>(*args)
}
