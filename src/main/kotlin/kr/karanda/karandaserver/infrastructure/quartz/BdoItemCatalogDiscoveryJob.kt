package kr.karanda.karandaserver.infrastructure.quartz

import kr.karanda.karandaserver.service.BdoItemCatalogService
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

/**
 * 카탈로그 발견 잡: 1시간마다 서브카테고리 하나를 조회해 신규 아이템을 감지한다.
 * 함께 orphan(claim 후 미완료) 행을 재플래그해 크래시 복구를 수행한다.
 */
@Component
class BdoItemCatalogDiscoveryJob : QuartzJobBean() {
    @Autowired
    private var bdoItemCatalogService: BdoItemCatalogService? = null

    override fun executeInternal(context: JobExecutionContext) {
        bdoItemCatalogService?.reflagOrphanedEnrichments()
        bdoItemCatalogService?.discoverNextCategory()
    }
}
