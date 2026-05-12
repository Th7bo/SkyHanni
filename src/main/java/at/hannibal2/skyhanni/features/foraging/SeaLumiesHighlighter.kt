package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawHitbox
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SeaPickleBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Uses a local cache instead of [SkyHanniBlockHighlighter]: that utility skips cleanup when its
 * highlight predicate is false, so blocks could stay highlighted with a stale pickle count after
 * changing config or leaving the area until the predicate toggled again.
 */
@SkyHanniModule
object SeaLumiesHighlighter {

    private val config get() = SkyHanniMod.feature.foraging.seaLumiesHighlight

    private var tick = 0

    private val targetBlocks = mutableSetOf<LorenzVec>()
    private val targetLock = Any()

    private fun minPickles(): Int = config.minPicklesPerBlock.coerceIn(1, 4)

    @HandleEvent(SkyHanniTickEvent::class, onlyOnIsland = IslandType.GALATEA)
    fun onTick(@Suppress("UNUSED_PARAMETER") event: SkyHanniTickEvent) {
        if (!config.enabled) {
            synchronized(targetLock) { targetBlocks.clear() }
            return
        }
        if (tick++ % 5 != 0) return

        val minPickles = minPickles()
        val radius = config.searchRadius.toInt().coerceAtLeast(1)
        val center = playerLocation().roundToBlock()
        val found = BlockUtils.nearbyBlocks(
            center,
            distance = radius,
            radius = radius,
            filter = Blocks.SEA_PICKLE,
        )
        synchronized(targetLock) {
            targetBlocks.clear()
            for ((loc, state) in found) {
                if (seaPickleCount(state) >= minPickles) {
                    targetBlocks.add(loc.roundToBlock())
                }
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!IslandType.GALATEA.isInIsland() || !config.enabled) return

        val minPickles = minPickles()
        val searchRadius = config.searchRadius
        val snapshot = synchronized(targetLock) { targetBlocks.toList() }
        val baseColor = config.color.toColor()
        val outlineAlpha = (baseColor.alpha * config.outlineOpacity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        val outlineColor = baseColor.addAlpha(outlineAlpha)
        val lineWidth = config.hitboxLineWidth.coerceIn(1, 5)
        for (loc in snapshot) {
            if (loc.distanceToPlayer() > searchRadius) continue
            val state = loc.getBlockStateAt()
            if (seaPickleCount(state) < minPickles) continue
            val aabb = loc.boundingToOffset(1.0, 1.0, 1.0).inflate(0.001)
            event.drawHitbox(aabb, outlineColor, lineWidth = lineWidth, depth = true)
        }
    }

    @HandleEvent
    fun onIslandLeave(event: IslandLeaveEvent) {
        if (event.island != IslandType.GALATEA) return
        synchronized(targetLock) { targetBlocks.clear() }
    }

    @HandleEvent
    fun onWorldChange() {
        synchronized(targetLock) { targetBlocks.clear() }
    }
}

private fun seaPickleCount(state: BlockState): Int {
    if (state.block != Blocks.SEA_PICKLE) return 0
    if (!state.hasProperty(SeaPickleBlock.PICKLES)) return 0
    return state.getValue(SeaPickleBlock.PICKLES)
}
