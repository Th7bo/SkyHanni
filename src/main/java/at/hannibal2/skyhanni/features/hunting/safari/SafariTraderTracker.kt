package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

/**
 * Watches the roaming Hunter NPCs' shard for item trades.
 *
 * Their dialog is only shown to whoever clicked them, so an offer routinely goes unused while a partymate is
 * carrying exactly the item it wants. Each complete offer is reported to chat, optionally posted to the party, and
 * kept for the display so it can still be found later in the run.
 *
 * Every NPC words both halves of the trade differently, so rather than four pairs of sentence patterns an offer is
 * any NPC line naming a shard, and a price is any NPC line asking for something in exchange. The pending offer is
 * held per speaker, so a completed trade is an offer plus the next price line from that same NPC.
 */
@SkyHanniModule
object SafariTraderTracker {

    private val config get() = SkyHanniMod.feature.hunting.safari.trades
    private val safariConfig get() = SkyHanniMod.feature.hunting.safari
    private val partyConfig get() = SkyHanniMod.feature.hunting.safari.party

    private val patternGroup = RepoPattern.group("hunting.safari.trader")

    /**
     * REGEX-TEST: [NPC] Huntress Melissa: Do you want this Macaw Shard? I already maxed that Attribute...
     * REGEX-TEST: [NPC] Hunter Billy: I found this really cool Gimmiegold Shard on the floor around here.
     */
    private val npcLinePattern by patternGroup.pattern(
        "npc-line",
        "\\[NPC] (?<npc>[^:]{1,40}): (?<text>.*)",
    )

    /**
     * REGEX-TEST: How about I give you it in exchange for, say, an Orange Gem?
     * REGEX-TEST: I'll trade it to you in exchange for a Blue Gem.
     * REGEX-TEST: You can h-h-have it if you give m-m-me a Purple Gem...
     */
    private val pricePattern by patternGroup.pattern(
        "price",
        ".*(?:in exchange for(?:,\\s*say,)?|give m-m-me)\\s+(?:an?|the)\\s+(?<item>.+?)\\s*[.!?…]*",
    )

    /**
     * REGEX-TEST: Party > [MVP+] Th7bo: Hunter Dennis (Icy -106 87 -7): Gimmiegold Shard for Purple Gem
     * REGEX-TEST: Party > Jaegerss: Huntress Melissa (Forest 12 70 -3): Macaw Shard for Orange Gem
     */
    private val partyLinePattern by patternGroup.pattern(
        "party-line",
        "Party > (?:\\[[^]]+] )?(?<sender>\\w{1,16}): (?<text>.+)",
    )

    /**
     * REGEX-TEST: Hunter Dennis (Icy -106 87 -7): Gimmiegold Shard for Purple Gem
     * REGEX-TEST: Hunter Dennis (-106 87 -7): Gimmiegold Shard for Purple Gem
     */
    private val sharedTradePattern by patternGroup.pattern(
        "shared-trade",
        "(?<npc>.+?) \\((?:(?<biome>[A-Za-z]+) )?(?<x>-?\\d+) (?<y>-?\\d+) (?<z>-?\\d+)\\): " +
            "(?<critter>.+?) Shard for (?<item>.+)",
    )

    /** The same offer re-read by clicking the NPC again should not re-announce. */
    private val repeatCooldown = 5.minutes

    /** Bounds the display; the oldest trade drops off beyond this. */
    private const val MAX_TRACKED = 6

    private const val SCAN_INTERVAL_TICKS = 20

    /** Where an offer was found. */
    data class Spot(val position: LorenzVec, val biome: SafariBiome?) {
        fun describe(): String {
            val coords = "${position.x.toInt()} ${position.y.toInt()} ${position.z.toInt()}"
            return biome?.let { "${it.displayName} $coords" } ?: coords
        }

        fun distance() = position.distanceToPlayer()
    }

    /** One resolved trade: [npc] hands over [critter]'s shard for [item]. */
    data class Trade(val npc: String, val critter: SafariCritter, val item: String, val spot: Spot?)

    private val pendingOffers = mutableMapOf<String, SafariCritter>()

    /** Speaker -> where their offer was found, until the price line completes it. */
    private val offerSpots = mutableMapOf<String, Spot>()

    /**
     * Where each Hunter was last seen. An NPC talks for several seconds and you are free to walk off mid sentence,
     * so by the time a line worth recording arrives the NPC can be out of range, or gone from the client entirely.
     */
    private val lastSeen = mutableMapOf<String, Spot>()

    private val announced = mutableMapOf<String, SimpleTimeMark>()

    /** Trades found this run, newest last. */
    val found = mutableListOf<Trade>()

    /**
     * Keeps track of where the Hunters are standing, continuously rather than only when one speaks, so a trade can
     * be placed at the NPC even if the dialog finishes after you have walked away from it.
     */
    @OptIn(AllEntitiesGetter::class)
    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(SCAN_INTERVAL_TICKS)) return
        for (entity in EntityUtils.getAllEntities()) {
            val name = entity.cleanName.takeIf { it.isNotEmpty() } ?: continue
            if (!isHunter(name)) continue
            lastSeen[name] = spotAt(entity.getLorenzVec().roundToBlock())
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        if (acceptShared(message)) return

        npcLinePattern.matchMatcher(message) {
            val npc = group("npc").trim()
            val text = group("text")

            pricePattern.matchMatcher(text) {
                val critter = pendingOffers.remove(npc) ?: return
                val item = group("item").trim()
                if (item.isEmpty()) return
                complete(Trade(npc, critter, item, offerSpots.remove(npc)))
                return
            }

            // Not a price, so see whether it is this NPC offering a shard. "Shard" alone is not enough, since
            // plenty of NPC flavour text mentions shards without offering one, so it also has to name a species.
            if ("Shard" !in text) return
            val critter = SafariCritter.findIn(text) ?: return
            pendingOffers[npc] = critter
            // Noted as the dialog opens: the price line follows seconds later, by which point the player may have
            // turned away from the NPC.
            locate(npc)?.let { offerSpots[npc] = it }
        }
    }

    private fun complete(trade: Trade) {
        val key = "${trade.npc}|${trade.critter.displayName}|${trade.item}"
        val last = announced[key]
        if (last != null && last.passedSince() < repeatCooldown) return
        announced[key] = SimpleTimeMark.now()

        // Recorded even when the chat report is off, so the display still works for anyone who would rather not
        // have the chat lines.
        record(trade)

        val text = describe(trade)
        if (config.report) {
            if (safariConfig.tradeShare) {
                var shared = false
                ChatUtils.clickableChat(
                    "§b${trade.npc}§f: ${trade.critter.coloredName} Shard §7for §e${trade.item}",
                    onClick = { if (!shared) shared = share(text) },
                    hover = "§eClick to share in party chat!",
                )
            } else {
                ChatUtils.chat("§b${trade.npc}§f: ${trade.critter.coloredName} Shard §7for §e${trade.item}")
            }
        }
        SafariEncounterAlerts.post(partyConfig.trades, text)
    }

    /** Returns whether the trade was actually sent. */
    private fun share(text: String): Boolean {
        if (!PartyApi.isInParty()) {
            ChatUtils.userError("You are not in a party!")
            return false
        }
        HypixelCommands.partyChat(text)
        return true
    }

    private fun describe(trade: Trade): String {
        val where = trade.spot?.let { " (${it.describe()})" }.orEmpty()
        return "${trade.npc}$where: ${trade.critter.displayName} Shard for ${trade.item}"
    }

    /**
     * Takes a trade a partymate's copy of SkyHanni announced.
     *
     * Their client saw the dialog and worked out where the NPC was; that is exactly the information this one is
     * missing, since the dialog is only ever shown to whoever clicked.
     *
     * @return true if the line was a shared trade and should not be parsed further
     */
    private fun acceptShared(message: String): Boolean {
        partyLinePattern.matchMatcher(message) {
            if (!partyConfig.acceptSharedTrades) return false
            // Our own announcement comes back to us; the local copy is already recorded, and was placed from the
            // entity rather than from text.
            if (group("sender") == PlayerUtils.getName()) return true
            return readSharedTrade(group("text"))
        }
        return false
    }

    private fun readSharedTrade(text: String): Boolean {
        sharedTradePattern.matchMatcher(text) {
            val critter = SafariCritter.byName(group("critter")) ?: return false
            val position = LorenzVec(group("x").toInt(), group("y").toInt(), group("z").toInt())
            val spot = Spot(position, SafariBiome.byDisplayName(group("biome")))
            record(Trade(group("npc"), critter, group("item"), spot))
            return true
        }
        return false
    }

    /** Adds a trade to the display, replacing whatever that NPC was last offering. */
    private fun record(trade: Trade) {
        found.removeIf { it.npc == trade.npc }
        found.add(trade)
        while (found.size > MAX_TRACKED) found.removeAt(0)
    }

    /**
     * Where to send people, in order of how well it is known: the NPC itself if it is still loaded, else where it
     * was last seen, else where the player is standing.
     */
    @OptIn(AllEntitiesGetter::class)
    private fun locate(npc: String): Spot? {
        for (entity in EntityUtils.getAllEntities()) {
            val name = entity.cleanName
            if (name.isNotEmpty() && npc in name) return spotAt(entity.getLorenzVec().roundToBlock())
        }
        lastSeen.entries.firstOrNull { npc in it.key }?.let { return it.value }
        // Last resort: the player has walked off, so this is only roughly where the NPC was.
        return spotAt(LocationUtils.playerLocation().roundToBlock())
    }

    /** Builds a spot, taking the biome from the position itself rather than the player's. */
    private fun spotAt(position: LorenzVec) =
        Spot(position, SafariAreaApi.biomeAt(position) ?: SafariAreaApi.currentBiome)

    /** The four roaming traders are all Hunters or Huntresses. */
    private fun isHunter(name: String) = name.startsWith("Hunter ") || name.startsWith("Huntress ")

    /** Clears half seen dialog, the repeat guard and the display; called when a run starts. */
    fun reset() {
        pendingOffers.clear()
        offerSpots.clear()
        lastSeen.clear()
        announced.clear()
        found.clear()
    }
}
