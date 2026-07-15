package kr.karanda.karandaserver.infrastructure.quartz

import kr.karanda.karandaserver.service.BdoItemCatalogService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

/**
 * 카탈로그 보강 잡: 보강 필요 행 1건을 채우고 이미지 미업로드 행 1건을 업로드한다.
 * 발견보다 잦은 주기로 돌려 신규 유입을 빠르게 소화한다.
 */
@Component
class BdoItemCatalogEnrichmentJob : QuartzJobBean() {
    @Autowired
    private var bdoItemCatalogService: BdoItemCatalogService? = null

    override fun executeInternal(context: JobExecutionContext) {
        bdoItemCatalogService?.enrichNext()
        bdoItemCatalogService?.uploadNextImage()
    }
}
