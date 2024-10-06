package kr.karanda.karandaserver.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import kr.karanda.karandaserver.data.DiscordProperties
import kr.karanda.karandaserver.data.TokenProperties
import kr.karanda.karandaserver.data.TradeMarketProperties
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service

@Service
@DependsOn("FirebaseConfiguration")
class FireStoreService {
    private val db: Firestore = FirestoreClient.getFirestore()
    private var discordProperties: DiscordProperties? = null
    private var tokenProperties: TokenProperties? = null

    fun getDiscordProperties(): DiscordProperties {
        if (this.discordProperties == null) {
            discordProperties = getDefaultData("discord").toObject(DiscordProperties::class.java)
        }
        return discordProperties!!
    }

    fun getTokenProperties(): TokenProperties {
        if (tokenProperties == null) {
            tokenProperties = getDefaultData("token").toObject(TokenProperties::class.java)
        }
        return tokenProperties!!
    }

    fun getTradeMarketProperties(): TradeMarketProperties {
        val properties = getDefaultData("trade-market").toObject(TradeMarketProperties::class.java)
        return properties!!
    }

    fun getTradeMarketLastUpdated(): Int {
        val data = getSynchronizeData("trade-market").toObject(TradeMarket::class.java)
        return data!!.lastUpdatedItemID.toInt()
    }

    fun setTradeMarketLastUpdated(itemNum: Int) {
        val data = TradeMarket(lastUpdatedItemID = itemNum.toString())
        db.collection("synchronize-data").document("trade-market").set(data)
    }

    private fun getDefaultData(name: String): DocumentSnapshot {
        val docRef = db.collection("defaultData").document(name)
        val document = docRef.get().get()
        if (document.exists()) {
            return document
        } else {
            throw Exception()
        }
    }

    private fun getSynchronizeData(name: String): DocumentSnapshot {
        val docRef = db.collection("synchronize-data").document(name)
        val document = docRef.get().get()
        if (document.exists()) {
            return document
        } else {
            throw Exception()
        }
    }
}

private data class TradeMarket(
    var lastUpdatedItemID: String = ""
)
