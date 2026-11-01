package kr.karanda.karandaserver.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS, reason = "Too many requests")
class TooManyRequestsException : RuntimeException()
