package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariRunTrackerConfig.SafariScope
import kotlin.time.Duration

/** Shared bits of the three Safari displays. */
object SafariDisplayUtils {

    private val config get() = SkyHanniMod.feature.hunting.safari.runTracker

    private const val BAR_WIDTH = 10

    /** Whether the displays should be up where the player is standing right now. */
    fun showsHere(): Boolean = when (config.showWhere) {
        SafariScope.EVERYWHERE -> true
        SafariScope.SAFARI_AND_ENTRANCE -> SafariAreaApi.inSafariOrEntrance
        SafariScope.SAFARI -> SafariAreaApi.inSafari
    }

    /** A progress bar as text, e.g. `§a██████░░░░ §f24/37`. */
    fun bar(label: String, current: Int, total: Int, labelColor: String, valueColor: String): String {
        val filled = if (total <= 0) 0 else (current * BAR_WIDTH / total).coerceIn(0, BAR_WIDTH)
        val bar = "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled)
        return "$labelColor$label §8[$valueColor$bar§8] $valueColor$current§7/$total"
    }

    fun formatDuration(duration: Duration): String {
        val seconds = duration.inWholeSeconds
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
