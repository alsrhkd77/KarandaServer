package kr.karanda.karandaserver.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import kr.karanda.karandaserver.properties.ItemImageProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 아이템 이미지를 pearlcdn에서 받아 GCP Storage 버킷에 업로드한다.
 * 인증은 firebase-admin과 공유하는 자격증명으로 구성된 [Storage] 빈을 사용한다(FirebaseConfiguration).
 */
@Component
class ItemImageStorage(
    private val storage: Storage,
    private val properties: ItemImageProperties,
) {
    private val client: RestClient = RestClient.create()

    /**
     * `${sourceUrlBase}${itemNum}.png`를 받아 `${pathPrefix}${itemNum}.png` 오브젝트로 업로드한다.
     * 다운로드 실패(2xx 아님) 또는 응답 없음이면 예외를 던진다(호출부에서 처리).
     */
    fun store(itemNum: Int) {
        val bytes = client.get()
            .uri("${properties.sourceUrlBase}${itemNum}.png")
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw IllegalStateException("Empty image response for item #$itemNum")

        val blobInfo = BlobInfo
            .newBuilder(BlobId.of(properties.bucket, "${properties.pathPrefix}${itemNum}.png"))
            .setContentType("image/png")
            .build()
        storage.create(blobInfo, bytes)
    }
}
