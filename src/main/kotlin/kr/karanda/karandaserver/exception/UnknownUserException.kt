package kr.karanda.karandaserver.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Unknown credentials")
class UnknownUserException: RuntimeException()