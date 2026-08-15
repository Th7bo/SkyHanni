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
 * matter of where it stands: the labelled entity stays loaded on the client the whole time, which is why the missing
 * list keeps reporting one nearby while nobody can see it. Marking that entity's position is therefore not a lookup
 * table of hiding spots, it is where it actually is, this run.
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

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        if (!config.hideyhoSolver) return
        val seen = SafariEntities.sightings
            .firstOrNull { it.critter == SafariCritter.HIDEYHO }
            ?.body?.getLorenzVec()?.roundToBlock()
        live = seen != null
        if (seen != null) position = seen
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        // Only what Hideyho itself says counts. Quoted in someone's chat, the same words would drop a mark that is
        // still good, which is why the speaker is part of the pattern.
        if (movedOrFoundPattern.matches(event.cleanMessage)) clear()
    }

    fun clear() {
        position = null
        live = false
    }

    /** A spot from the last run says nothing about this one. */
    fun reset() = clear()
}
