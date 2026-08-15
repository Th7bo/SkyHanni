package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Finds the hiding Hideyho.
 *
 * Hideyho hides somewhere in the Haunted biome and asks you to come and find it. The thing is that hiding is only a
 * matter of where it stands: the entity itself stays loaded on the client throughout, so marking its position is not
 * a lookup table of hiding spots, it is where it actually is, this run.
 *
 * What hiding *does* take away is the nametag, which is why this reads the skin instead - see
 * [SafariEntities.hideyhoLocation]. A hidden Hideyho has no nametag to sweep for at all, so the species sweep is
 * only the fallback here, for the case where it is standing in the open with its label up.
 *
 * What the client cannot do is see it before the server has sent it, and Hypixel only sends an entity once you are
 * within its tracking range. So the position is remembered after the entity unloads again: it does not move while
 * hidden, and one pass within range is then enough to keep the mark for the rest of the round. Its own chat lines
 * say when that stops being true, and the mark is dropped on either.
 *
 * This is the counterpart to [HideyhoFinder], which walks you around the known hiding spots instead - useful when
 * the client has never had the entity in range at all.
 */
@SkyHanniModule
object SafariHideyhoSolver {

    private val config get() = SkyHanniMod.feature.hunting.safari.highlights

    /**
     * REGEX-TEST: [MOB] Hideyho: No peeking! Come find me!
     * REGEX-TEST: [MOB] Hideyho: Aah! You found me!
     * REGEX-TEST: [MOB] Hideyho: Hehe, you found me!
     */
    private val movedOrFoundPattern by RepoPattern.pattern(
        "hunting.safari.hideyho-solver.reset",
        "\\[MOB] Hideyho: .*(?:[Cc]ome find me!|No peeking!|[Yy]ou found me!).*",
    )

    /** Where it is, or was last seen. Null when nothing is known this round. */
    var position: LorenzVec? = null
        private set

    /** Whether it is loaded right now, as opposed to only remembered. */
    var live = false
        private set

    /**
     * Set once anyone has caught it. Only one Hideyho spawns per run, so nothing is coming back, and the entity can
     * still be loaded for a moment after the catch - without this the next sweep would put the mark straight back.
     */
    private var caught = false

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        if (!config.hideyhoSolver || caught) return
        val seen = SafariEntities.hideyhoLocation?.roundToBlock() ?: nametagged()
        live = seen != null
        if (seen != null) position = seen
    }

    /**
     * Someone has found it - `CAPTURE! You found the Hideyho...`, or the loot share line when a partymate did.
     * Either way the spot is finished with, so the mark goes.
     */
    fun onCatch(critter: SafariCritter) {
        if (critter != SafariCritter.HIDEYHO) return
        caught = true
        clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        // Only what Hideyho itself says counts. Quoted in someone's chat, the same words would drop a mark that is
        // still good, which is why the speaker is part of the pattern.
        if (movedOrFoundPattern.matches(event.cleanMessage)) clear()
    }

    /** Where a Hideyho that is standing in the open, label and all, is - which the skin lookup covers anyway. */
    private fun nametagged(): LorenzVec? = SafariEntities.sightings
        .firstOrNull { it.critter == SafariCritter.HIDEYHO }
        ?.body?.getLorenzVec()?.roundToBlock()

    fun clear() {
        position = null
        live = false
    }

    /** A spot from the last run says nothing about this one, and the next run has its own Hideyho to find. */
    fun reset() {
        caught = false
        clear()
    }
}
