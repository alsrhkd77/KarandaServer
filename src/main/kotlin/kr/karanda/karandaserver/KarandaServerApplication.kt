package kr.karanda.karandaserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class KarandaServerApplication

fun main(args: Array<String>) {
    runApplication<KarandaServerApplication>(*args)
}
