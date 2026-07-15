package kr.karanda.karandaserver

import kr.karanda.karandaserver.properties.AdventurerProfileUrlBase
import kr.karanda.karandaserver.properties.ItemImageProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AdventurerProfileUrlBase::class, ItemImageProperties::class)
@SpringBootApplication
class KarandaServerApplication

fun main(args: Array<String>) {
    runApplication<KarandaServerApplication>(*args)
}
