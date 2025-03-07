package kr.karanda.karandaserver.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_GATEWAY, reason = "Api is not available")
class ExternalApiException : RuntimeException()
