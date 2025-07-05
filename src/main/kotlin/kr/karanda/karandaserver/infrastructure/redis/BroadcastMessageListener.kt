package kr.karanda.karandaserver.infrastructure.redis

import kotlinx.serialization.json.Json
import kr.karanda.karandaserver.dto.BroadcastMessage
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class BroadcastMessageListener(private val messagingTemplate: SimpMessagingTemplate) : MessageListener {
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val value = Json.decodeFromString<List<BroadcastMessage>>(String(message.body))
        for (msg in value) {
            broadcastToWebsocket(msg)
        }
    }

    private fun broadcastToWebsocket(message: BroadcastMessage) {
        for (destination in message.destinations) {
            messagingTemplate.convertAndSend(destination, message.message)
        }
    }
}