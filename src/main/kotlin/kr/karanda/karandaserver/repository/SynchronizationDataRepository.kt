package kr.karanda.karandaserver.repository

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import org.springframework.core.env.Environment
import org.springframework.stereotype.Repository

@Repository
class SynchronizationDataRepository(
    private val firestore: Firestore,
    private val environment: Environment,
) {
    fun getTradeMarketLastUpdated(): Int {
        val document = collection().document("trade-market").get().get()
        if (document.exists()) {
            return document.toObject(TradeMarket::class.java)?.lastUpdatedItem?.toInt()
                ?: throw Exception("Firestore data does invalid! (trade-market)")
        } else {
            throw Exception("Firestore data does not exist! (trade-market)")
        }
    }

    fun setTradeMarketLastUpdated(itemNum: Int) {
        collection().document("trade-market").update("lastUpdatedItem", itemNum.toString())
    }

    fun getTradeMarketPriceLastUpdated(): Int {
        val document = collection().document("trade-market").get().get()
        if (document.exists()) {
            return document.toObject(TradeMarket::class.java)?.priceLastUpdated?.toInt()
                ?: throw Exception("Firestore data does invalid! (trade-market)")
        } else {
            throw Exception("Firestore data does not exist! (trade-market)")
        }
    }

    fun setTradeMarketPriceLastUpdated(itemNum: Int) {
        collection().document("trade-market").update("priceLastUpdated", itemNum.toString())
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
    var lastUpdatedItemID: String = "",
    var lastUpdatedItem: String = "",
    var priceLastUpdated: String = "",
)
