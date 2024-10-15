package kr.karanda.karandaserver.dto

data class BDOFamilyVerificationRequest(
    val region: String,
    val familyName: String,
    val code: String,
)
