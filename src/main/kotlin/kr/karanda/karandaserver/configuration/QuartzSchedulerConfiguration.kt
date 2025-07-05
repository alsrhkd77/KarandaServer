package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.infrastructure.quartz.MarketCleanUpOldDataJob
import kr.karanda.karandaserver.infrastructure.quartz.MarketLatestPriceDataUpdateJob
import kr.karanda.karandaserver.infrastructure.quartz.PublishMarketWaitListJob
import kr.karanda.karandaserver.infrastructure.quartz.TranslateItemDataJob
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
    fun marketLatestPriceDataUpdateJobDetail(): JobDetail {
        return JobBuilder
            .newJob(MarketLatestPriceDataUpdateJob::class.java)
            .withIdentity("marketLatestPriceDataUpdateJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun marketLatestPriceDataUpdateTrigger(@Qualifier("marketLatestPriceDataUpdateJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("marketLatestDataUpdateTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
            .build()
    }

    @Bean
    fun publishMarketWaitListJobDetail(): JobDetail {
        return JobBuilder
            .newJob(PublishMarketWaitListJob::class.java)
            .withIdentity("publishMarketWaitListJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun publishMarketWaitListTrigger(@Qualifier("publishMarketWaitListJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("publishMarketWaitListTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
            .build()
    }

    @Bean
    fun marketCleanUpOldDataJobDetail(): JobDetail {
        return JobBuilder
            .newJob(MarketCleanUpOldDataJob::class.java)
            .withIdentity("marketCleanUpOldDataJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun marketCleanUpOldDataTrigger(@Qualifier("marketCleanUpOldDataJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("marketCleanUpOldDataTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInHours(24).repeatForever())
            .build()
    }

    @Bean
    fun translateItemDataJobDetail(): JobDetail {
        return JobBuilder
            .newJob(TranslateItemDataJob::class.java)
            .withIdentity("translateItemDataJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Profile("production")
    @Bean
    fun translateItemDataTrigger(@Qualifier("translateItemDataJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("translateItemDataTrigger", "tradeMarket")
            .startNow()
            .withSchedule(simpleSchedule().withIntervalInMinutes(1).repeatForever())
            .build()
    }
}