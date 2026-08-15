package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.data.ProfileStorageData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Every run this profile has finished, kept across restarts in the profile storage.
 *
 * A run is written the moment it ends, which is also when it stops being the run the panels show. Nothing is ever
 * written mid run: a run in progress is still changing, and half of one is not worth keeping.
 */
object SafariRunHistory {

    /** Enough for months of play; the storage stays small and the stats stay honest. */
    private const val MAX_RUNS = 500

    /** Runs with nothing in them are noise - leaving and re-entering makes plenty. */
    private const val MIN_CATCHES = 1

    private val storage get() = ProfileStorageData.profileSpecific?.hunting

    /** Saved runs, oldest first. */
    val runs: List<SafariRunRecord> get() = storage?.safariRuns.orEmpty()

    val size get() = runs.size

    /** One species' record across every saved run. */
    data class SpeciesStat(val critter: SafariCritter, val total: Int, val runsSeen: Int, val best: Int) {
        /** Average per run over the runs it turned up in, not over every run. */
        val perRunSeen get() = if (runsSeen == 0) 0.0 else total.toDouble() / runsSeen
    }

    /**
     * Saves a finished run.
     *
     * An empty run is dropped. Walking through the entrance and out again produces one, and a history full of those
     * buries the runs that happened.
     */
    fun record(session: SafariRunSession) {
        val list = storage?.safariRuns ?: return
        val record = SafariRunRecord.of(session)
        if (record.partyTotal() < MIN_CATCHES) return

        list.add(record)
        while (list.size > MAX_RUNS) list.removeAt(0)
    }

    fun clear() {
        storage?.safariRuns?.clear()
    }

    /** Saved runs that kept a shard breakdown, and so can be priced. */
    fun pricedRuns() = runs.count { it.hasShardData() }

    // --- stats ---

    /** Every species with its totals across the saved runs. */
    fun speciesStats(): List<SpeciesStat> = SafariCritter.entries.map { critter ->
        var total = 0
        var runsSeen = 0
        var best = 0
        for (run in runs) {
            val caught = run.caught(critter)
            if (caught == 0) continue
            total += caught
            runsSeen++
            best = maxOf(best, caught)
        }
        SpeciesStat(critter, total, runsSeen, best)
    }

    fun statFor(critter: SafariCritter): SpeciesStat =
        speciesStats().firstOrNull { it.critter == critter } ?: SpeciesStat(critter, 0, 0, 0)

    fun totalTime(): Duration = runs.sumOf { it.durationMillis }.milliseconds

    fun totalCatches() = runs.sumOf { it.partyTotal() }

    fun ownCatches() = runs.sumOf { it.ownTotal() }

    fun totalShards() = runs.sumOf { it.totalShards }

    /** The best party dex any saved run reached. */
    fun bestDex() = runs.maxOfOrNull { it.partyUnique() } ?: 0

    /** How many runs went all the way to every species. */
    fun perfectRuns() = runs.count { it.partyUnique() == SafariCritter.total }

    /** Species that have never once been caught in a saved run. */
    fun neverCaught(): List<SafariCritter> = speciesStats().filter { it.total == 0 }.map { it.critter }
}
