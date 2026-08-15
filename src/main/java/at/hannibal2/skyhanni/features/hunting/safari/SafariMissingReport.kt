package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod

/**
 * Formats a run's outstanding species as lines meant to be read by teammates:
 *
 * ```
 * Icy missing: Wumpa, Troodon (1/3)
 * Cavern missing: Gemzie (1/3)
 * ```
 *
 * Biomes that are already complete are omitted rather than printed empty, so the message stays short enough for one
 * chat line per biome.
 */
object SafariMissingReport {

    private val config get() = SkyHanniMod.feature.hunting.safari.runTracker

    /** One line per biome that still has uncaught species; empty if the dex is done. */
    fun lines(session: SafariRunSession): List<String> = SafariBiome.entries.mapNotNull { biome ->
        val missing = session.missing(biome)
        if (missing.isEmpty()) return@mapNotNull null
        // Comma separated, because a species and its count are both spaced and "Troodon (1/3) Gemzie" would read
        // as one thing.
        val species = missing.joinToString(", ") { critter ->
            when {
                session.isUnavailable(critter) -> "${critter.displayName} (none)"
                // Tells the reader how many are left, not just that something is outstanding.
                session.required(critter) > 1 && !config.uniqueOnly ->
                    "${critter.displayName} (${session.partyCatches(critter)}/${session.required(critter)})"
                else -> critter.displayName
            }
        }
        "${biome.displayName} missing: $species"
    }

    /**
     * The same report as one string, for the clipboard.
     *
     * Joined with a visible separator rather than newlines: the clipboard is nearly always pasted straight into
     * chat, and the chat box drops line breaks, which ran the sections together.
     */
    fun text(session: SafariRunSession): String {
        val lines = lines(session)
        if (lines.isEmpty()) return "All ${SafariCritter.total} critters caught!"
        return lines.joinToString(" | ")
    }
}
