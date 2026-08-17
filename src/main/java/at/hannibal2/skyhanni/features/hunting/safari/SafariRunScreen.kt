package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.milliseconds

/**
 * The run screen: this run in detail, every saved run, and the totals across all of them.
 *
 * The three tabs answer three different questions - what is left to catch right now, how this run compares to the
 * ones before it, and what the whole history adds up to - so they are tabs rather than one very long panel.
 */
class SafariRunScreen : SkyHanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.hunting.safari

    enum class Tab(val displayName: String) {
        RUN("Run"),
        HISTORY("History"),
        STATS("Stats"),
    }

    private var tab = openTab

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val left = (width - SIZE_X) / 2
        val top = (height - SIZE_Y) / 2

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(left.toFloat(), top.toFloat())
            GuiRenderUtils.drawRect(0, 0, SIZE_X, SIZE_Y, BACKGROUND)

            Renderable.withMousePosition(mouseX - left, mouseY - top) {
                DrawContextUtils.pushPop {
                    DrawContextUtils.translate(PADDING.toFloat(), PADDING.toFloat())
                    tabRow().renderXAligned(mouseX - left - PADDING, mouseY - top - PADDING, SIZE_X - PADDING * 2)
                }
                DrawContextUtils.pushPop {
                    DrawContextUtils.translate(PADDING.toFloat(), (PADDING + TAB_HEIGHT).toFloat())
                    val content = Renderable.vertical(lines().map { Renderable.text(it) }, spacing = 1)
                    content.renderXAligned(
                        mouseX - left - PADDING,
                        mouseY - top - PADDING - TAB_HEIGHT,
                        SIZE_X - PADDING * 2,
                    )
                }
            }
        }
    }

    private fun tabRow(): Renderable = Renderable.horizontal(spacing = 6) {
        for (entry in Tab.entries) {
            val label = if (entry == tab) "§e§l${entry.displayName}" else "§7${entry.displayName}"
            add(Renderable.clickable("§8[$label§8]", onLeftClick = { switchTab(entry) }))
        }
        val session = SafariRunManager.currentOrLast
        if (session != null) {
            add(
                Renderable.clickable(
                    "§8[§bCopy missing§8]",
                    onLeftClick = { OSUtils.copyToClipboard(SafariMissingReport.text(session)) },
                    tips = listOf("§eCopies what is still uncaught to the clipboard."),
                ),
            )
            add(
                Renderable.clickable(
                    "§8[§bShare§8]",
                    onLeftClick = {
                        for (line in SafariMissingReport.lines(session)) {
                            SafariEncounterAlerts.post(config.party.missingList, line)
                        }
                    },
                    tips = listOf("§ePosts the missing list where the party settings say."),
                ),
            )
        }
    }

    private fun switchTab(entry: Tab) {
        tab = entry
        openTab = entry
    }

    private fun lines(): List<String> = when (tab) {
        Tab.RUN -> runLines()
        Tab.HISTORY -> historyLines()
        Tab.STATS -> statsLines()
    }

    private fun runLines(): List<String> {
        val session = SafariRunManager.currentOrLast ?: return listOf("§7No Critter Safari run tracked yet.")
        return buildList {
            val live = SafariRunManager.currentSession != null
            val state = if (live) "live" else "last run"
            add("§6§lCritter Safari §7($state) §f${SafariDisplayUtils.formatDuration(session.elapsed())}")
            add(
                "§7Party §f${session.partyUnique()}§7/${SafariCritter.total}   " +
                    "§7You §f${session.ownUnique()}   §7Catches §f${session.partyTotal()}   " +
                    "§7Capsules §f${session.totalAttempts()}",
            )
            if (config.profit.enabled && session.totalShards() > 0) {
                add("§7Shards §f${session.totalShards()}   §7Value §6${SafariShardValue.coinsText(session)}")
            }
            add("")

            for (biome in SafariBiome.entries) {
                add(
                    "${biome.coloredName} §8- §f${session.partyUnique(biome)}§7/${SafariCritter.totalIn(biome)} " +
                        "§8(${session.partyTotal(biome)} catches)",
                )
                for (critter in SafariCritter.inBiome(biome)) {
                    add(critterLine(session, critter))
                }
                add("")
            }

            val perPlayer = session.uniquePerPlayer()
            if (perPlayer.size > 1) {
                add("§6Per player")
                for ((player, counts) in perPlayer) {
                    val text = counts.entries.sortedByDescending { it.value }
                        .joinToString(", ") { "${it.key.displayName} ${it.value}" }
                    add("§b$player§7: $text")
                }
            }
        }
    }

    private fun critterLine(session: SafariRunSession, critter: SafariCritter): String {
        val caught = session.partyCatches(critter)
        val required = session.required(critter)
        val mark = when {
            session.isUnavailable(critter) -> "§8none this run"
            session.isComplete(critter) -> "§a✔"
            required > 1 -> "§e$caught§7/$required"
            else -> "§c✖"
        }
        val catchers = session.catchersOf(critter)
        val by = if (catchers.isEmpty()) "" else " §8by ${catchers.joinToString(", ")}"
        val attempts = session.attempts(critter).takeIf { it > 0 }?.let { " §8($it thrown)" }.orEmpty()
        return "  ${critter.coloredName} $mark$by$attempts"
    }

    private fun historyLines(): List<String> {
        val runs = SafariRunHistory.runs
        if (runs.isEmpty()) return listOf("§7No saved Critter Safari runs yet.")
        return buildList {
            add("§6§l${runs.size} saved runs")
            add("§8dex · catches · length · value")
            for (run in runs.reversed().take(MAX_HISTORY_LINES)) {
                val length = SafariDisplayUtils.formatDuration(run.durationMillis.milliseconds)
                val value = if (run.hasShardData()) {
                    "§6${SafariShardValue.formatCoins(SafariShardValue.valueOf(run))}"
                } else {
                    "§8no shard data"
                }
                add(
                    "§f${run.partyUnique()}§7/${SafariCritter.total} §8· §f${run.partyTotal()} §8· " +
                        "§7$length §8· $value",
                )
            }
        }
    }

    private fun statsLines(): List<String> {
        val runs = SafariRunHistory.runs
        if (runs.isEmpty()) return listOf("§7No saved Critter Safari runs yet.")
        return buildList {
            add("§6§lTotals across ${runs.size} runs")
            add("§7Time played §f${SafariRunHistory.totalTime().format()}")
            add("§7Catches §f${SafariRunHistory.totalCatches()} §8(yours ${SafariRunHistory.ownCatches()})")
            add(
                "§7Best dex §f${SafariRunHistory.bestDex()}§7/${SafariCritter.total} §8· " +
                    "§7full runs §f${SafariRunHistory.perfectRuns()}",
            )
            if (config.profit.enabled) {
                val value = SafariShardValue.totalValue(runs)
                add(
                    "§7Shards §f${SafariRunHistory.totalShards()} §8· §7worth §6" +
                        "${SafariShardValue.formatCoins(value)} coins §8(${SafariRunHistory.pricedRuns()} priced)",
                )
            }
            add("")
            add("§8species · total · runs seen · best · per run")
            for (stat in SafariRunHistory.speciesStats().sortedByDescending { it.total }) {
                val perRun = ((stat.perRunSeen * 10).toInt() / 10.0).toString()
                add(
                    "  ${stat.critter.coloredName} §f${stat.total} §8· §7${stat.runsSeen} §8· " +
                        "§7${stat.best} §8· §7$perRun",
                )
            }
        }
    }

    companion object {
        private const val SIZE_X = 420
        private const val SIZE_Y = 260
        private const val PADDING = 8
        private const val TAB_HEIGHT = 14
        private const val BACKGROUND = 0xC0101010.toInt()
        private const val MAX_HISTORY_LINES = 18

        private var openTab = Tab.RUN

        fun open() {
            DelayedRun.runNextTick {
                MinecraftCompat.screen = SafariRunScreen()
            }
        }
    }
}
