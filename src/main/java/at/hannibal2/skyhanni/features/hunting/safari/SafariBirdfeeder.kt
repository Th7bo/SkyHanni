package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

/**
 * Calls out a Macaw the moment one turns up.
 *
 * The Macaw is the one species a run is not guaranteed to produce at all, which is why "everything except Macaw" is
 * usually the real finish line. So a spawn is worth telling the party about: whoever is in another biome wants to
 * know there is something to come back for.
 *
 * Two ways of noticing one, because neither covers the other: the Birdfeeder announces it, which arrives whether or
 * not you are anywhere near, and a Macaw label appearing in the world, which covers one that turns up without the
 * Birdfeeder line and comes with coordinates to send. A cooldown covers the overlap.
 */
@SkyHanniModule
object SafariBirdfeeder {

    private val config get() = SkyHanniMod.feature.hunting.safari.alerts

    /**
     * REGEX-TEST: A Bluebird was attracted to the Birdfeeder!
     * REGEX-TEST: Two Macaws were attracted to the Birdfeeder!
     */
    private val birdfeederPattern by RepoPattern.pattern(
        "hunting.safari.birdfeeder",
        "\\S+ (?<bird>[A-Za-z ]+?)s? (?:were|was) attracted to the Birdfeeder!",
    )

    /** Long enough that the two ways of noticing the same spawn cannot both fire. */
    private val cooldown = 1.minutes

    private var wasLoaded = false
    private var lastAnnounced = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        birdfeederPattern.matchMatcher(event.cleanMessage) {
            // A wording this loose matches anything shaped like the sentence, so the species has to be one the
            // roster knows before it is announced as a spawn.
            val bird = SafariCritter.byName(group("bird").trim()) ?: return
            if (bird == SafariCritter.MACAW) {
                announce(null)
            } else if (config.birdfeeder) {
                // The commoner birds get the same call in their own rarity's color, so a Bluebird cannot be
                // mistaken for the thing you were waiting for.
                TitleManager.sendTitle("${bird.rarity.chatColorCode}§l${bird.displayName.uppercase()}!", "at the Birdfeeder")
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        val where = SafariEntities.sightings
            .firstOrNull { it.critter == SafariCritter.MACAW }
            ?.body?.getLorenzVec()?.roundToBlock()

        // Edge triggered: one announcement when it appears, not one per scan while it is standing there.
        val loaded = where != null
        if (loaded && !wasLoaded) announce(where)
        wasLoaded = loaded
    }

    private fun announce(where: LorenzVec?) {
        if (lastAnnounced.passedSince() < cooldown) return
        lastAnnounced = SimpleTimeMark.now()

        val description = where?.let {
            "${SafariBiome.FOREST.displayName} ${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()}"
        }
        SafariEncounterAlerts.onMacawSpawn(description)
    }

    /** A Macaw from the last run says nothing about this one. */
    fun reset() {
        wasLoaded = false
        lastAnnounced = SimpleTimeMark.farPast()
    }
}
