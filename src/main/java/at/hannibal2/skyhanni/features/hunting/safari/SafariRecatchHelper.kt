package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.phys.AABB
import kotlin.time.Duration.Companion.seconds

/**
 * Remembers where a critter was when you threw at it, so you can be waiting for it.
 *
 * Throwing a capsule takes the critter out of the world. If the capture fails it comes back to the spot it left from
 * and starts running again, and the fast ones are gone before you have found them a second time. The spot is known
 * the moment the throw lands, so it gets pinned at the critter's own size and the next capsule can be in the air
 * before the critter is.
 *
 * Not every species is worth it. Commons are caught on the throw; Hideyho, Wumpa and Doomspiral do not come straight
 * back; and a Snoozle never went anywhere in the first place.
 */
@SkyHanniModule
object SafariRecatchHelper {

    private val config get() = SkyHanniMod.feature.hunting.safari.highlights

    /** Dropped after this long with nothing resolving it, so a stale box cannot linger. */
    private val holdDuration = 40.seconds

    /** A sighting older than this is not what the throw hit. */
    private val sightingDuration = 10.seconds

    /** Within a scan or two, so the species counts as being in the world right now. */
    private val loadedDuration = 1.seconds

    /** Once it is this far from the pin it is running again and the pin means nothing. */
    private const val STALE_DISTANCE = 5.0

    /** Added to the score of anything behind the player, so it always loses to what is not. */
    private const val BEHIND_PENALTY = 1000.0

    /** Species a pin does nothing for, whatever their rarity. */
    private val neverPinned = setOf(
        SafariCritter.HIDEYHO,
        SafariCritter.WUMPA,
        SafariCritter.DOOMSPIRAL,
        SafariCritter.SNOOZLE,
    )

    private data class Seen(val box: AABB, val time: SimpleTimeMark)

    private val lastSeen = mutableMapOf<SafariCritter, Seen>()

    /** The sweep these sightings came from, so a cached list is not re-timestamped. */
    private var lastScan = SimpleTimeMark.farPast()

    var pinnedCritter: SafariCritter? = null
        private set
    var pinnedBox: AABB? = null
        private set
    private var pinnedAt = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        if (SafariEntities.lastScan != lastScan) {
            lastScan = SafariEntities.lastScan
            trackSightings()
        }

        val box = pinnedBox ?: return
        val critter = pinnedCritter ?: return

        if (pinnedAt.passedSince() > holdDuration) {
            clear()
            return
        }

        // Back and running: the critter is loaded again and has left the spot, so the spot is now just a place it
        // used to be.
        val seen = lastSeen[critter] ?: return
        if (seen.time.passedSince() >= loadedDuration) return
        if (seen.box.center.toLorenzVec().distance(box.center.toLorenzVec()) > STALE_DISTANCE) clear()
    }

    private fun trackSightings() {
        val now = SimpleTimeMark.now()
        val best = mutableMapOf<SafariCritter, Double>()
        for (sighting in SafariEntities.sightings) {
            // A capsule mid capture carries the critter's name too, and pinning it puts the mark on the ball rather
            // than on the spot the critter will come back to. A real critter has a mob under its nametag.
            val mob = sighting.mob ?: continue
            val box = mob.boundingBox
            val score = aimScore(box)
            val current = best[sighting.critter]
            // Several of a species can be in view at once, and only one of them was thrown at. The one nearest the
            // line you are looking along is the one.
            if (current != null && current <= score) continue
            best[sighting.critter] = score
            lastSeen[sighting.critter] = Seen(box, now)
        }
    }

    /** Reacts to a parsed catch event. */
    fun onCatchEvent(event: SafariCatchEvent) {
        if (!config.recatchHelper) return
        when (event.type) {
            SafariEventType.ATTEMPT -> pin(event.critter)
            // The throw failed, so it is coming back to where it was. Hold the pin and restart the clock rather
            // than letting the throw's timer run it out.
            SafariEventType.FAILED -> if (event.critter == pinnedCritter) pinnedAt = SimpleTimeMark.now()
            SafariEventType.OWN_CATCH -> if (event.critter == pinnedCritter) clear()
            else -> {}
        }
    }

    /**
     * How far a box is off the line the player is looking along - lower is more likely to be what a capsule was
     * aimed at. Anything behind the player scores by plain distance instead, pushed out beyond anything in front.
     */
    private fun aimScore(box: AABB): Double {
        val player = MinecraftCompat.localPlayerOrNull ?: return Double.MAX_VALUE
        val toBox = box.center.subtract(player.eyePosition)
        val look = player.getViewVector(1.0f)
        val along = toBox.dot(look)
        if (along <= 0) return BEHIND_PENALTY + toBox.length()
        return toBox.subtract(look.scale(along)).length()
    }

    private fun worthPinning(critter: SafariCritter) =
        critter.rarity != LorenzRarity.COMMON && critter !in neverPinned

    private fun pin(critter: SafariCritter) {
        if (!worthPinning(critter)) {
            clear()
            return
        }
        val seen = lastSeen[critter]
        // A species nobody has seen recently cannot be pinned: the throw was at something out of the client's view,
        // and guessing a spot would be worse than showing none.
        if (seen == null || seen.time.passedSince() > sightingDuration) {
            clear()
            return
        }
        pinnedCritter = critter
        pinnedBox = seen.box
        pinnedAt = SimpleTimeMark.now()
    }

    fun clear() {
        pinnedCritter = null
        pinnedBox = null
    }

    /** Distance from the player to the pin, or null when there is nothing pinned. */
    fun distance(): Double? = pinnedBox?.let { LocationUtils.playerLocation().distance(it.center.toLorenzVec()) }

    /** Forgotten between runs; a spot from the last one is meaningless in this one. */
    fun reset() {
        lastSeen.clear()
        clear()
    }
}
