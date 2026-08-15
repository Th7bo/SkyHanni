package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.level.block.Blocks

/**
 * Tracks the bee nests that Honeybugs come from.
 *
 * Punching one leaves the block exactly as it was, so there is nothing to read back afterwards. The punch itself is
 * therefore what gets recorded. The consequence is that a nest someone else punched still shows as outstanding,
 * since this client never saw it happen.
 *
 * Unlike the Cavern walls these have no fixed positions, so the known set grows as the Forest is explored: the count
 * is "nests you have come across", not "nests on the map". That is still the actionable reading, since an unpunched
 * nest is somewhere you can walk to.
 */
@SkyHanniModule
object SafariNestTracker {

    private val config get() = SkyHanniMod.feature.hunting.safari

    /** Sweeping is comparatively expensive, so it runs on a timer rather than per tick. */
    private const val SCAN_INTERVAL_TICKS = 40
    private const val SCAN_RADIUS = 24


    private val known = linkedSetOf<LorenzVec>()
    private val punched = mutableSetOf<LorenzVec>()

    data class Nest(val position: LorenzVec, val unpunched: Boolean, val distance: Double)

    private fun isEnabled() = config.missing.showNests || config.highlights.nests

    /** Records a punch on a nest. */
    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onBlockClick(event: BlockClickEvent) {
        val position = event.position
        if (position.getBlockAt() != Blocks.BEE_NEST) return
        known.add(position)
        punched.add(position)
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(SCAN_INTERVAL_TICKS)) return
        if (!isEnabled()) return
        if (SafariAreaApi.currentBiome != SafariBiome.FOREST) return

        val center = LocationUtils.playerLocation().roundToBlock()
        known.addAll(BlockUtils.nearbyBlocks(center, SCAN_RADIUS, filter = Blocks.BEE_NEST).keys)
    }

    /** Every nest found so far, still to punch ones first, then by distance. */
    fun nests(): List<Nest> = known
        // An unloaded chunk reports air, which would read as punched, so only a loaded chunk can say either way.
        .filter { it.isInLoadedChunk() }
        .map { Nest(it, it !in punched, it.distanceToPlayer()) }
        .sortedWith(compareBy({ !it.unpunched }, { it.distance }))

    fun unpunchedCount() = nests().count { it.unpunched }

    /** Nests are per instance, so what was found last run means nothing in this one. */
    fun reset() {
        known.clear()
        punched.clear()
    }
}
