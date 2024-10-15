package kr.karanda.karandaserver.configuration

import kr.karanda.karandaserver.quartz.MarketDataUpdateJob
import kr.karanda.karandaserver.quartz.PublishMarketWaitListJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuartzSchedulerConfiguration {
    @Bean
    fun marketDataUpdateJobDetail(): JobDetail {
        return JobBuilder
            .newJob(MarketDataUpdateJob::class.java)
            .withIdentity("marketUpdateJob", "tradeMarket")
            .storeDurably()
            .build()
    }

    @Bean
    fun marketDataUpdateTrigger(@Qualifier("marketDataUpdateJobDetail") jobDetail: JobDetail): Trigger {
        return TriggerBuilder
            .newTrigger()
            .forJob(jobDetail)
            .withIdentity("marketUpdateTrigger", "tradeMarket")
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
}