package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format

/**
 * `/shsafari` and its subcommands. The run, history and stats screens live behind the bare command; everything
 * that is meant to be read by somebody else is a subcommand, since it ends up in chat or on the clipboard.
 */
@SkyHanniModule
object SafariCommands {

    private val config get() = SkyHanniMod.feature.hunting.safari

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsafari") {
            description = "Opens the Critter Safari run screen, or reports the run in chat."
            aliases = listOf("shcritters")
            category = CommandCategory.USERS_ACTIVE

            simpleCallback { SafariRunScreen.open() }

            literalCallback("text") { summary() }
            literalCallback("missing") { missing() }
            literalCallback("players") { players() }
            literalCallback("copy") { copy() }
            literalCallback("share") { share() }
            literalCallback("stats") { stats() }
            literalCallback("history") { history() }
            literalCallback("reset") {
                SafariRunManager.reset()
                ChatUtils.chat("Safari run reset.")
            }
            literalCallback("testalert") { SafariEncounterAlerts.test() }
        }
    }

    private fun session() = SafariRunManager.currentOrLast

    private fun summary() {
        val session = session() ?: run {
            ChatUtils.userError("No Critter Safari run tracked yet.")
            return
        }
        ChatUtils.chat("§6Critter Safari §7- ${SafariDisplayUtils.formatDuration(session.elapsed())}")
        ChatUtils.chat("§7Party: §f${session.partyUnique()}§7/${SafariCritter.total} §8· §7You: §f${session.ownUnique()}")
        for (biome in SafariBiome.entries) {
            val caught = session.partyUnique(biome)
            ChatUtils.chat("${biome.coloredName}§7: §f$caught§7/${SafariCritter.totalIn(biome)}")
        }
        if (config.profit.enabled && session.totalShards() > 0) {
            ChatUtils.chat("§7Value: §6${SafariShardValue.coinsText(session)} §8(${session.totalShards()} shards)")
        }
    }

    private fun missing() {
        val session = session() ?: run {
            ChatUtils.userError("No Critter Safari run tracked yet.")
            return
        }
        val lines = SafariMissingReport.lines(session)
        if (lines.isEmpty()) {
            ChatUtils.chat("§aAll ${SafariCritter.total} critters caught!")
            return
        }
        for (line in lines) ChatUtils.chat(line)
    }

    private fun players() {
        val session = session() ?: run {
            ChatUtils.userError("No Critter Safari run tracked yet.")
            return
        }
        val perPlayer = session.uniquePerPlayer()
        if (perPlayer.isEmpty()) {
            ChatUtils.chat("Nobody has caught anything yet this run.")
            return
        }
        for ((player, counts) in perPlayer) {
            val text = counts.entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key.displayName} ${it.value}" }
            ChatUtils.chat("§b$player§7: $text")
        }
    }

    private fun copy() {
        val session = session() ?: run {
            ChatUtils.userError("No Critter Safari run tracked yet.")
            return
        }
        OSUtils.copyToClipboard(SafariMissingReport.text(session))
        ChatUtils.chat("Copied the missing list to the clipboard.")
    }

    private fun share() {
        val session = session() ?: run {
            ChatUtils.userError("No Critter Safari run tracked yet.")
            return
        }
        for (line in SafariMissingReport.lines(session)) {
            SafariEncounterAlerts.post(config.party.missingList, line)
        }
    }

    private fun stats() {
        val runs = SafariRunHistory.runs
        if (runs.isEmpty()) {
            ChatUtils.chat("No saved Critter Safari runs yet.")
            return
        }
        ChatUtils.chat("§6Critter Safari §7- ${runs.size} saved runs")
        ChatUtils.chat("§7Time played: §f${SafariRunHistory.totalTime().format()}")
        ChatUtils.chat("§7Catches: §f${SafariRunHistory.totalCatches()} §8(yours: ${SafariRunHistory.ownCatches()})")
        ChatUtils.chat(
            "§7Best dex: §f${SafariRunHistory.bestDex()}§7/${SafariCritter.total} " +
                "§8· full runs: ${SafariRunHistory.perfectRuns()}",
        )
        ChatUtils.chat("§7Shards: §f${SafariRunHistory.totalShards()}")
        if (config.profit.enabled) {
            val value = SafariShardValue.totalValue(runs)
            ChatUtils.chat(
                "§7Total value: §6${SafariShardValue.formatCoins(value)} coins " +
                    "§8(${SafariRunHistory.pricedRuns()} priced runs)",
            )
        }
        val never = SafariRunHistory.neverCaught()
        if (never.isNotEmpty()) {
            ChatUtils.chat("§7Never caught: §f${never.joinToString(", ") { it.displayName }}")
        }
    }

    private fun history() {
        val runs = SafariRunHistory.runs.takeLast(10)
        if (runs.isEmpty()) {
            ChatUtils.chat("No saved Critter Safari runs yet.")
            return
        }
        ChatUtils.chat("§6Last ${runs.size} Critter Safari runs")
        for (run in runs.reversed()) {
            val value = if (run.hasShardData()) {
                " §8· §6${SafariShardValue.formatCoins(SafariShardValue.valueOf(run))}"
            } else {
                ""
            }
            ChatUtils.chat(
                "§7${run.partyUnique()}§8/${SafariCritter.total} §7dex §8· §f${run.partyTotal()} §7catches$value",
            )
        }
    }
}
