package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType.SAFARI
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SafariTradeShare {

    private val config get() = SkyHanniMod.feature.hunting.safari

    private val patternGroup = RepoPattern.group("hunting.safari.trade-share")

    /**
     * REGEX-TEST: [NPC] Huntress Melissa: Do you want this Macaw Shard? I already maxed that Attribute...
     * REGEX-TEST: [NPC] Hunter Melissa: Do you want this Sea Emperor Shard? I already maxed that Attribute…
     */
    private val tradeOfferPattern by patternGroup.pattern(
        "offer",
        "\\[NPC] (?<npc>[^:]+): Do you want this (?<reward>[^?]+)\\? I already maxed that Attribute.*",
    )

    /**
     * REGEX-TEST: [NPC] Huntress Melissa: How about I give you it in exchange for, say, an Orange Gem?
     * REGEX-TEST: [NPC] Hunter Melissa: How about I give you it in exchange for, say, a Blue Gem?
     */
    private val tradeCostPattern by patternGroup.pattern(
        "cost",
        "\\[NPC] (?<npc>[^:]+): How about I give you it in exchange for, say, an? (?<cost>[^?]+)\\?.*",
    )

    // How long the reward line stays valid while waiting for the price line of the same trade.
    private val OFFER_TIMEOUT = 30.seconds

    // A safari can hold several hunters, so offers are tracked per NPC instead of in a single slot.
    private val pendingOffers = mutableMapOf<String, PendingOffer>()
    private val announcedTrades = mutableSetOf<String>()

    private class PendingOffer(val reward: String, val time: SimpleTimeMark = SimpleTimeMark.now())

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.tradeShare) return
        val message = event.cleanMessage

        tradeOfferPattern.matchMatcher(message) {
            pendingOffers[group("npc")] = PendingOffer(group("reward"))
            return
        }

        tradeCostPattern.matchMatcher(message) {
            val npc = group("npc")
            val offer = pendingOffers.remove(npc) ?: return
            if (offer.time.passedSince() > OFFER_TIMEOUT) return
            announceTrade(npc, offer.reward, group("cost"))
        }
    }

    private fun announceTrade(npc: String, reward: String, cost: String) {
        val trade = "$npc: $reward for $cost"
        if (!announcedTrades.add(trade)) return

        // Captured now, as the player may have walked away from the NPC by the time they click.
        val location = findNpcLocation(npc)

        // The NPC only offers the trade on the first interaction, so a click that failed to send must stay clickable.
        var shared = false
        ChatUtils.clickableChat(
            "§d$npc §fgives §6$reward §ffor §6$cost§f!",
            onClick = {
                if (!shared) shared = shareTrade(location, trade)
            },
            hover = "§eClick to share in party chat!",
        )
    }

    private fun findNpcLocation(npc: String): LorenzVec = MobData.displayNpcs
        .filter { it.name == npc }
        .minByOrNull { it.distanceToPlayer() }
        ?.getLorenzVec() ?: LocationUtils.playerLocation()

    /** Returns whether the trade was actually sent. */
    private fun shareTrade(location: LorenzVec, trade: String): Boolean {
        if (!PartyApi.isInParty()) {
            ChatUtils.userError("You are not in a party!")
            return false
        }
        HypixelCommands.partyChat("${location.toChatFormat()} | $trade")
        return true
    }

    @HandleEvent
    private fun onWorldChange() {
        pendingOffers.clear()
        announcedTrades.clear()
    }
}
