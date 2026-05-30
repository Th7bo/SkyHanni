package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.now
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.getDurationOrNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose

@SkyHanniModule
object ForgeDisplay {

    private val config get() = SkyHanniMod.feature.mining.forgeDisplay
    private val storage get() = ProfileStorageData.profileSpecific?.mining

    private val patternGroup = RepoPattern.group("mining.forgedisplay")

    /**
     * REGEX-TEST: §7Time Remaining: §b3h 20m
     * REGEX-TEST: §7Time Remaining: §b1d 4h
     */
    private val timeRemainingPattern by patternGroup.pattern(
        "timeremaining",
        "Time Remaining: (?<time>.+)",
    )

    private const val FORGE_INVENTORY_NAME = "The Forge"

    private var display = emptyList<String>()

    data class ForgeProcess(
        @Expose val internalName: NeuInternalName,
        @Expose val finishTime: SimpleTimeMark,
    )

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.enabled) return
        if (event.inventoryName != FORGE_INVENTORY_NAME) return
        val storage = storage ?: return

        val newSlots = mutableMapOf<Int, ForgeProcess>()
        for ((slot, item) in event.inventoryItems) {
            val internalName = item.getInternalNameOrNull() ?: continue
            for (line in item.getLore()) {
                val time = timeRemainingPattern.matchMatcher(line.removeColor()) {
                    group("time")
                } ?: continue
                val duration = getDurationOrNull(time) ?: continue
                newSlots[slot] = ForgeProcess(internalName, now() + duration)
                break
            }
        }
        storage.forgeSlots = newSlots
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) {
            display = emptyList()
            return
        }
        val slots = storage?.forgeSlots.orEmpty()
        if (slots.isEmpty()) {
            display = emptyList()
            return
        }

        display = buildList {
            add("§6§lForge")
            for (process in slots.values.sortedBy { it.finishTime }) {
                val name = process.internalName.repoItemName
                val timeLeft = process.finishTime - now()
                val timeText = if (timeLeft.isNegative()) "§aReady!" else "§e${timeLeft.format()}"
                add(" §7- $name§7: $timeText")
            }
        }
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (display.isEmpty()) return
        config.position.renderStrings(display, posLabel = "Forge Display")
    }

    private fun onMiningIsland() =
        IslandType.DWARVEN_MINES.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland()

    private fun isEnabled() = config.enabled &&
        SkyBlockUtils.inSkyBlock &&
        (config.showEverywhere || onMiningIsland())
}