package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.math.roundToInt

/**
 * The Hunter trades found this run, nearest first.
 *
 * The chat report scrolls away; this keeps the offers to hand, so a quest item picked up later can still be spent on
 * one found half an hour earlier.
 */
@SkyHanniModule
object SafariTradeDisplay {

    private val config get() = SkyHanniMod.feature.hunting.safari.trades

    private var display = emptyList<String>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSecondPassed() {
        display = if (isEnabled()) buildDisplay() else emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStrings(display, posLabel = "Safari Hunter Trades")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.tracker && SafariDisplayUtils.showsHere()

    private fun buildDisplay(): List<String> {
        val trades = SafariTraderTracker.found
        if (trades.isEmpty()) return emptyList()

        return buildList {
            add("§6§lHunter Trades")
            // Nearest first, so the one worth walking to is at the top.
            for (trade in trades.sortedBy { it.spot?.distance() ?: Double.MAX_VALUE }) {
                add("${trade.critter.coloredName} §7← §e${trade.item}")
                add("  §8${describe(trade)}")
            }
        }
    }

    /** `Icy -106 87 -7 · 34m`, dropping whatever is unknown. */
    private fun describe(trade: SafariTraderTracker.Trade): String {
        val spot = trade.spot ?: return trade.npc
        return "${spot.describe()} · ${spot.distance().roundToInt()}m"
    }
}
