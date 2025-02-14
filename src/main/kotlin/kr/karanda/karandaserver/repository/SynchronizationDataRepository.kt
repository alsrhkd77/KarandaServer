package kr.karanda.karandaserver.repository

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import jakarta.annotation.PostConstruct
import kr.karanda.karandaserver.dto.BroadcastMessage
import org.springframework.core.env.Environment
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Repository

@Repository
class SynchronizationDataRepository(
    private val firestore: Firestore,
    private val environment: Environment,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @PostConstruct
    fun initialize() {
        listenBroadcastMessage()
    }

    @Async
    fun broadcast(broadcastMessage: BroadcastMessage) {
        collection().document("broadcast").set(broadcastMessage)
    }

    fun getTradeMarketLastUpdated(): Int {
        val document = collection().document("trade-market").get().get()
        if (document.exists()) {
            return document.toObject(TradeMarket::class.java)?.lastUpdatedItemID?.toInt()
                ?: throw Exception("Firestore data does invalid! (trade-market)")
        } else {
            throw Exception("Firestore data does not exist! (trade-market)")
        }
    }

    fun setTradeMarketLastUpdated(itemNum: Int) {
        val data = TradeMarket(lastUpdatedItemID = itemNum.toString())
        collection().document("trade-market").set(data)
    }

    private fun listenBroadcastMessage() {
        collection().document("broadcast").addSnapshotListener { snapshot, e ->
            if (e != null) {
                println("Listen failed.\n${e.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(BroadcastMessage::class.java)?.let {
                    broadcastToWebsocket(it)
                }
            } else {
                println("Current data: null")
            }
        }
    }

    private fun broadcastToWebsocket(data: BroadcastMessage) {
        for (destination in data.destinations) {
            messagingTemplate.convertAndSend(destination, data.message)
        }
    }

    private fun getDocument(name: String): DocumentSnapshot {
        val document = collection().document(name).get().get()
        if (document.exists()) {
            return document
        } else {
            throw Exception()
        }
    }

    private fun collection(): CollectionReference {
        if (environment.activeProfiles.contains("develop")) {
            return firestore.collection("synchronize-data-for-dev")
        }
        return firestore.collection("synchronize-data")
    }
}

private data class TradeMarket(
    var lastUpdatedItemID: String = ""
)
