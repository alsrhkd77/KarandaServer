package kr.karanda.karandaserver.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import kr.karanda.karandaserver.dto.DiscordProperties
import kr.karanda.karandaserver.dto.SafeBrowsingApiProperties
import kr.karanda.karandaserver.dto.TokenProperties
import kr.karanda.karandaserver.dto.TradeMarketProperties
import org.springframework.stereotype.Repository

@Repository
class DefaultDataRepository(private val firestore: Firestore) {
    private var discordProperties: DiscordProperties? = null
    private var tokenProperties: TokenProperties? = null
    private var tradeMarketProperties: TradeMarketProperties? = null
    private var safeBrowsingApiProperties: SafeBrowsingApiProperties? = null

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
        if (tradeMarketProperties == null) {
            tradeMarketProperties = getDefaultData("trade-market").toObject(TradeMarketProperties::class.java)
        }
        return tradeMarketProperties!!
    }

    fun getSafeBrowsingApiProperties(): SafeBrowsingApiProperties {
        if (safeBrowsingApiProperties == null) {
            safeBrowsingApiProperties =
                getDefaultData("safe-browsing-api").toObject(SafeBrowsingApiProperties::class.java)
        }
        return safeBrowsingApiProperties!!
    }

    private fun getDefaultData(name: String): DocumentSnapshot {
        val docRef = firestore.collection("defaultData").document(name)
        val document = docRef.get().get()
        if (document.exists()) {
            return document
        } else {
            throw Exception()
        }
    }
}