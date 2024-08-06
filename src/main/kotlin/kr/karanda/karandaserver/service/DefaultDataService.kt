package kr.karanda.karandaserver.service

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import kr.karanda.karandaserver.data.DiscordProperties
import kr.karanda.karandaserver.data.TokenProperties
import kr.karanda.karandaserver.data.TradeMarketProperties
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service

@DependsOn("FirebaseConfiguration")
@Service
class DefaultDataService {
    private val db: Firestore = FirestoreClient.getFirestore()
    private var discordProperties: DiscordProperties? = null
    private var tokenProperties: TokenProperties? = null

    fun getDiscordProperties(): DiscordProperties{
        if(this.discordProperties == null){
            discordProperties = getDocument("discord").toObject(DiscordProperties::class.java)
        }
        return discordProperties!!
    }

    fun getTokenProperties(): TokenProperties {
        if(tokenProperties == null) {
            tokenProperties = getDocument("token").toObject(TokenProperties::class.java)
        }
        return tokenProperties!!
    }

    fun getTradeMarketProperties(): TradeMarketProperties {
        val properties = getDocument("trade-market").toObject(TradeMarketProperties::class.java)
        return properties!!
    }

    private fun getDocument(name: String) : DocumentSnapshot {
        val docRef = db.collection("defaultData").document(name)
        val document = docRef.get().get()
        if (document.exists()) {
            return document
        } else {
            throw Exception()
        }
    }
}