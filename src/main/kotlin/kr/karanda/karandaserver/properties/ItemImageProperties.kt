package kr.karanda.karandaserver.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 아이템 이미지 원본 CDN 및 GCP Storage 업로드 위치 설정.
 *
 * - sourceUrlBase: pearlcdn 이미지 원본 경로. `${sourceUrlBase}${itemNum}.png`로 다운로드.
 * - bucket / pathPrefix: 업로드 대상. `${pathPrefix}${itemNum}.png` 오브젝트로 저장.
 */
@ConfigurationProperties(prefix = "karanda.item-image")
data class ItemImageProperties(
    var sourceUrlBase: String,
    var bucket: String,
    var pathPrefix: String,
)
