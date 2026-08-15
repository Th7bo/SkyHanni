package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockUtils

/**
 * Party and personal dex progress for this run, a bar per biome, and who is covering which biome.
 */
@SkyHanniModule
object SafariProgressDisplay {

    private val config get() = SkyHanniMod.feature.hunting.safari.runTracker
    private val profitConfig get() = SkyHanniMod.feature.hunting.safari.profit

    private var display = emptyList<String>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSecondPassed() {
        display = if (isEnabled()) buildDisplay() else emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTick() {
        // The run timer would otherwise only move once a second, which reads as a stuck display.
        if (!isEnabled() || display.isEmpty()) return
        display = buildDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStrings(display, posLabel = "Safari Progress")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && SafariDisplayUtils.showsHere()

    private fun buildDisplay(): List<String> = buildList {
        // Standing at the entrance before going in, no run has been tracked yet. Showing an empty tracker is right
        // there: it says the feature is watching and gives the biome targets.
        val session = SafariRunManager.currentOrLast
        val live = SafariRunManager.currentSession != null
        val total = SafariCritter.total

        add(
            when {
                session == null -> "§6§lCritter Safari §7(ready)"
                live -> "§6§lCritter Safari §f${SafariDisplayUtils.formatDuration(session.elapsed())}"
                else -> "§6§lCritter Safari §7(last run)"
            },
        )

        val partyUnique = session?.partyUnique() ?: 0
        val ownUnique = session?.ownUnique() ?: 0
        add(SafariDisplayUtils.bar("Party", partyUnique, total, "§7", if (partyUnique == total) "§a" else "§f"))
        add(SafariDisplayUtils.bar("You", ownUnique, total, "§7", "§b"))

        // Named a few seconds into the run and different for everyone in the party, so it is worth its own line.
        SafariHotspot.biome?.takeIf { config.showHotspot }?.let {
            add("§7Hotspot: ${it.coloredName}")
        }

        for (biome in SafariBiome.entries) {
            val caught = session?.partyUnique(biome) ?: 0
            val max = SafariCritter.totalIn(biome)
            val complete = caught == max
            val label = if (complete) "${biome.displayName} ✔" else biome.displayName
            add(SafariDisplayUtils.bar(label, caught, max, biome.color.getChatColor(), if (complete) "§a" else biome.color.getChatColor()))
        }

        // Left off until there is a shard to price: "0 coins" says nothing true.
        if (profitConfig.enabled && profitConfig.showOnDisplay && (session?.totalShards() ?: 0) > 0) {
            add("§7Value: §6${SafariShardValue.coinsText(session)}")
        }

        if (config.showPerPlayer && session != null) {
            val perPlayer = session.uniquePerPlayer()
            if (perPlayer.size > 1) {
                for ((player, counts) in perPlayer) {
                    add("§b$player§8: §7${describe(counts)}")
                }
            }
        }
    }

    /** A player's coverage as `Icy 9, Haunted 2`, busiest biome first. */
    private fun describe(counts: Map<SafariBiome, Int>): String = counts.entries
        .sortedByDescending { it.value }
        .joinToString(", ") { "${it.key.displayName} ${it.value}" }
        .ifEmpty { "-" }
}
