package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.infrastructure.quartz.BdoItemCatalogDiscoveryJob
import kr.karanda.karandaserver.infrastructure.quartz.BdoItemCatalogEnrichmentJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class QuartzSchedulerConfiguration {
    @Bean
    fun bdoItemCatalogDiscoveryJobDetail(): JobDetail {
        return JobBuilder
            .newJob(BdoItemCatalogDiscoveryJob::class.java)
            .withIdentity("bdoItemCatalogDiscoveryJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun bdoItemCatalogDiscoveryTrigger(@Qualifier("bdoItemCatalogDiscoveryJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("bdoItemCatalogDiscoveryTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever())
            .build()
    }

    @Bean
    fun bdoItemCatalogEnrichmentJobDetail(): JobDetail {
        return JobBuilder
            .newJob(BdoItemCatalogEnrichmentJob::class.java)
            .withIdentity("bdoItemCatalogEnrichmentJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun bdoItemCatalogEnrichmentTrigger(@Qualifier("bdoItemCatalogEnrichmentJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("bdoItemCatalogEnrichmentTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInMinutes(1).repeatForever())
            .build()
    }
}
