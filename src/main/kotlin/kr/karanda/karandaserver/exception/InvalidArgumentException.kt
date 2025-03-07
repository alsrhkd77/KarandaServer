package kr.karanda.karandaserver.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Argument is invalid")
class InvalidArgumentException: RuntimeException()