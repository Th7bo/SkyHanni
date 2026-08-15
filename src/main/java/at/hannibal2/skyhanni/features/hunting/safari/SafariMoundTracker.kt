package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Rockmite mounds: how many have been broken, and where the ones still standing are.
 *
 * Breaking one announces itself in chat, so the count needs nothing read out of the world. Across the sample logs
 * that split 80 empty to 23 with a Rockmite, so "how many are left" was never the useful figure - what matters is
 * how many have been broken, since each is another roll.
 *
 * Finding the standing ones is harder: they are interaction entities rather than blocks, and plenty of other things
 * wear an interaction box. Three rules together do the work - on the Cavern floor, centred on a block (a mound is
 * placed on the map while a falling fish is at whatever fraction of a block it has reached), and with nothing inside
 * it (a mound is only the box, while a fish is a mob the box is drawn around).
 */
@SkyHanniModule
object SafariMoundTracker {

    private val patternGroup = RepoPattern.group("hunting.safari.mounds")

    /**
     * REGEX-TEST: The mound falls apart, but nothing is inside...
     */
    private val emptyMoundPattern by patternGroup.pattern(
        "empty",
        "The mound falls apart, but nothing is inside.*",
    )

    /**
     * REGEX-TEST: The mound fell apart, revealing a Rockmite hidden inside!
     */
    private val rockmiteMoundPattern by patternGroup.pattern(
        "rockmite",
        "The mound fell apart, revealing.*",
    )

    /** Mounds vary; these bracket the one measured example with room either side. */
    private const val MIN_WIDTH = 0.35
    private const val MAX_WIDTH = 1.10
    private const val MIN_HEIGHT = 0.25
    private const val MAX_HEIGHT = 0.95
    private const val SCAN_RADIUS = 64.0

    /** Mounds are on the Cavern floor, which is below this. */
    private const val MAX_Y = 65.0

    /** How far off a block's center the box may sit and still count as placed on it. */
    private const val CENTRE_TOLERANCE = 0.05

    /** How close a creature has to be to count as being inside a box rather than near it. */
    private const val INSIDE_RADIUS = 0.35
    private const val INSIDE_HEIGHT = 1.0

    /** Both the display and the highlights ask every frame; the answer changes far slower. */
    private val cacheDuration = 500.milliseconds

    var broken = 0
        private set
    var rockmites = 0
        private set

    private var cached: List<LorenzVec> = emptyList()
    private var cachedAt = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        // "Spider Mound" in Torrhus Canyon is unrelated; these two wordings are the only ones that mean a Rockmite
        // mound has just been finished off.
        when {
            emptyMoundPattern.matches(message) -> broken++
            rockmiteMoundPattern.matches(message) -> {
                broken++
                rockmites++
            }
        }
    }

    /** Interaction entities near the player whose hitbox matches a mound. */
    fun mounds(): List<LorenzVec> {
        if (cachedAt.passedSince() < cacheDuration) return cached
        cachedAt = SimpleTimeMark.now()
        cached = scan()
        return cached
    }

    @OptIn(AllEntitiesGetter::class)
    private fun scan(): List<LorenzVec> {
        val nearby = EntityUtils.getAllEntities()
            .filter { it.distanceSqToPlayer() <= SCAN_RADIUS * SCAN_RADIUS }
            .toList()
        val creatures = nearby.filter { it.type != EntityType.INTERACTION && it is LivingEntity }
        val candidates = nearby.filter { it.type == EntityType.INTERACTION && looksLikeAMound(it) }

        return candidates.filter { !wrapsACreature(it, creatures) }.map { it.getLorenzVec().roundToBlock() }
    }

    private fun looksLikeAMound(entity: Entity): Boolean {
        if (entity.y > MAX_Y) return false
        val box = entity.boundingBox
        if (!inBand(box.maxX - box.minX, box.maxY - box.minY)) return false
        return isBlockCentred(entity)
    }

    private fun isBlockCentred(entity: Entity) =
        offsetFromCentre(entity.x) < CENTRE_TOLERANCE && offsetFromCentre(entity.z) < CENTRE_TOLERANCE

    private fun offsetFromCentre(coordinate: Double) = abs(coordinate - (floor(coordinate) + 0.5))

    /**
     * The fish that fall down the waterfalls are real entities that an interaction box is drawn around; a mound is
     * only the box. So anything with a mob sitting in it is not a mound.
     */
    private fun wrapsACreature(box: Entity, creatures: List<Entity>) = creatures.any {
        abs(it.x - box.x) <= INSIDE_RADIUS && abs(it.z - box.z) <= INSIDE_RADIUS && abs(it.y - box.y) <= INSIDE_HEIGHT
    }

    private fun inBand(width: Double, height: Double) =
        width in MIN_WIDTH..MAX_WIDTH && height in MIN_HEIGHT..MAX_HEIGHT && height <= width + 0.1

    fun reset() {
        broken = 0
        rockmites = 0
        cached = emptyList()
        cachedAt = SimpleTimeMark.farPast()
    }
}
