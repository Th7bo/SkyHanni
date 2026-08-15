package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariHighlightConfig.MarkStyle
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.Display
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.seconds

/**
 * Finds the drops lying on the floor.
 *
 * Hypixel builds one out of three string item displays sitting in a single block, and announces it with a particle a
 * block above. That count of three is what actually identifies one, since plenty of single item displays are
 * scenery, so this sweeps for the displays directly rather than listening for the particle.
 *
 * A drop stays listed for a few seconds after it stops being confirmed, so a display that flickers out of view does
 * not make the mark blink, and one that has been picked up disappears shortly after.
 */
@SkyHanniModule
object SafariFloorDrops {

    private val config get() = SkyHanniMod.feature.hunting.safari.highlights

    private const val SCAN_INTERVAL_TICKS = 20

    /** Exactly this many string displays make a drop. Fewer is scenery. */
    private const val STRING_DISPLAYS = 3

    private val holdDuration = 5.seconds

    private val confirmed = mutableMapOf<LorenzVec, SimpleTimeMark>()

    /** The floor drops currently known. */
    val positions: Set<LorenzVec> get() = confirmed.keys

    @OptIn(AllEntitiesGetter::class)
    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(SCAN_INTERVAL_TICKS)) return
        if (config.floorDrops == MarkStyle.OFF) {
            confirmed.clear()
            return
        }

        val strings = mutableMapOf<LorenzVec, Int>()
        for (entity in EntityUtils.getAllEntities()) {
            val display = entity as? Display.ItemDisplay ?: continue
            if (!display.isString()) continue
            val block = display.getLorenzVec().roundToBlock()
            strings[block] = strings.getOrDefault(block, 0) + 1
        }

        val now = SimpleTimeMark.now()
        for ((block, count) in strings) {
            if (count == STRING_DISPLAYS) confirmed[block] = now
        }
        confirmed.removeIf { (_, seen) -> seen.passedSince() > holdDuration }
    }

    /** Forgets a drop the player has just interacted with, without waiting for it to expire. */
    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onBlockClick(event: BlockClickEvent) {
        confirmed.remove(event.position)
    }

    private fun Display.ItemDisplay.isString(): Boolean {
        val stack = itemRenderState()?.itemStack() ?: return false
        return !stack.isEmpty && stack.item == Items.STRING
    }

    fun reset() {
        confirmed.clear()
    }
}
