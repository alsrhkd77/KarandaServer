package kr.karanda.karandaserver.controller

import io.swagger.v3.oas.annotations.tags.Tag
import kr.karanda.karandaserver.dto.BDOFamily
import kr.karanda.karandaserver.dto.BDOFamilyVerificationRequest
import kr.karanda.karandaserver.dto.User
import kr.karanda.karandaserver.service.BDOFamilyService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "BDO family verification", description = "BDO family verification API")
@RestController
@RequestMapping("/auth/bdo-family")
class BDOFamilyVerificationController(val bdoFamilyService: BDOFamilyService) {

    @PostMapping("/start-verification")
    fun startVerification(@RequestBody data: BDOFamilyVerificationRequest): BDOFamily {
        val authentication = SecurityContextHolder.getContext().authentication.principal as User
        val family = bdoFamilyService.startVerification(
            uuid = authentication.userUUID,
            familyName = data.familyName,
            region = data.region,
            code = data.code,
        )
        return family
        //TODO: 시작 성공, 올바르지 않은 닉네임, 올바르지 않은 코드(=파싱 실패), 이미 사용중인 코드(->빼앗기 위해 계속 진행)
    }
}