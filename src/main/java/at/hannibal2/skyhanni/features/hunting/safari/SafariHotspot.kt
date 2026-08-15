package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Watches which biome the run has made your Hunting Hotspot.
 *
 * Hypixel says so once, a few seconds after you walk in. It is *yours* - everyone in the party is told a different
 * one - which is exactly why it is worth saying out loud. Four people deciding who takes which biome have four
 * hotspots between them, and only their own client was told.
 */
@SkyHanniModule
object SafariHotspot {

    private val config get() = SkyHanniMod.feature.hunting.safari.alerts
    private val partyConfig get() = SkyHanniMod.feature.hunting.safari.party

    /**
     * REGEX-TEST: HOTSPOT! Your Hunting Hotspot is the Icy Biome!
     * REGEX-TEST: HOTSPOT! Your Hunting Hotspot is the Haunted Biome!
     */
    private val hotspotPattern by RepoPattern.pattern(
        "hunting.safari.hotspot",
        "HOTSPOT! Your Hunting Hotspot is the (?<biome>.+?) Biome!",
    )

    /** The biome that is your hotspot this run, or null before it is announced. */
    var biome: SafariBiome? = null
        private set

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        hotspotPattern.matchMatcher(event.cleanMessage) {
            // An unrecognised biome name means Hypixel has changed something; better to leave the display blank
            // than to report a guess.
            val found = SafariBiome.byDisplayName(group("biome")) ?: return
            biome = found
            announce(found)
        }
    }

    private fun announce(found: SafariBiome) {
        if (config.hotspot) {
            ChatUtils.chat("Your hotspot: ${found.coloredName}")
        }
        SafariEncounterAlerts.post(partyConfig.hotspot, "My hotspot: ${found.displayName}")
    }

    /** A hotspot belongs to one run; the next one is told its own. */
    fun reset() {
        biome = null
    }
}
