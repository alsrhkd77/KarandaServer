package kr.karanda.karandaserver

import kr.karanda.karandaserver.properties.AdventurerProfileUrlBase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@EnableConfigurationProperties(AdventurerProfileUrlBase::class)
@SpringBootApplication
class KarandaServerApplication

fun main(args: Array<String>) {
    runApplication<KarandaServerApplication>(*args)
}
