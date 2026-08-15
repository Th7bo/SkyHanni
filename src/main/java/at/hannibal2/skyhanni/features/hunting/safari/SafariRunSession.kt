package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration

/**
 * Tally for one Critter Safari run, from arrival until the player leaves.
 *
 * Everything is derived from a single table of "who caught what, how many times". Your own catches come from
 * `CAPTURE!` lines; partymates' catches come from the `LOOT SHARE!` line that names them. Because loot share
 * identifies the catcher, per player and per biome progress fall out for free, which is what makes the
 * "four players, one biome each" split checkable.
 *
 * The function count is high on purpose: there is one accessor per question the displays, the missing report and
 * the saved record each ask of a run.
 */
@Suppress("TooManyFunctions")
class SafariRunSession(val selfName: String, val startTime: SimpleTimeMark = SimpleTimeMark.now()) {

    private val config get() = SkyHanniMod.feature.hunting.safari.runTracker

    private val ownCatches = mutableMapOf<SafariCritter, Int>()
    private val attempts = mutableMapOf<SafariCritter, Int>()
    private val failures = mutableMapOf<SafariCritter, Int>()

    /** critter -> partymate name -> how many times they caught it. */
    private val sharedCatches = mutableMapOf<SafariCritter, MutableMap<String, Int>>()

    /**
     * How many of each species are loaded right now, replaced wholesale each scan.
     *
     * Deliberately not cumulative: a critter that escapes a capsule comes back as a new entity, so counting distinct
     * entities over time only ever climbs.
     */
    private var nearbyCounts = mapOf<SafariCritter, Int>()

    /**
     * Shards you actually received, by species, from your own catches and from loot share alike.
     *
     * Kept per species because a shard is only worth what its own species trades for. A catch is not one shard
     * either, so the catch counts cannot stand in for it.
     */
    val shards = mutableMapOf<SafariCritter, Int>()

    /** Species this run can no longer produce, so they stop being reported as outstanding. */
    var unavailable: Set<SafariCritter> = emptySet()

    var ownShards = 0
        private set
    var sharedShards = 0
        private set

    private var lastEvent = startTime

    fun record(event: SafariCatchEvent) {
        lastEvent = SimpleTimeMark.now()
        val critter = event.critter
        when (event.type) {
            SafariEventType.OWN_CATCH -> {
                ownCatches.addOrPut(critter, 1)
                ownShards += event.shards
                shards.addOrPut(critter, event.shards)
            }
            SafariEventType.SHARED_CATCH -> {
                val catcher = event.catcher ?: return
                sharedCatches.getOrPut(critter) { sortedMapOf() }.addOrPut(catcher, 1)
                sharedShards += event.shards
                shards.addOrPut(critter, event.shards)
            }
            SafariEventType.ATTEMPT -> attempts.addOrPut(critter, 1)
            SafariEventType.FAILED -> failures.addOrPut(critter, 1)
        }
    }

    private fun <K> MutableMap<K, Int>.addOrPut(key: K, amount: Int) {
        this[key] = getOrDefault(key, 0) + amount
    }

    // --- your progress ---

    fun caughtByYou(critter: SafariCritter) = ownCatches.getOrDefault(critter, 0) > 0

    fun ownUnique() = ownCatches.size

    fun ownUnique(biome: SafariBiome) = ownCatches.keys.count { it.biome == biome }

    fun ownTotal() = ownCatches.values.sum()

    fun ownTotal(biome: SafariBiome) = ownCatches.entries.filter { it.key.biome == biome }.sumOf { it.value }

    // --- party progress ---

    /** Replaces the live nearby counts with a fresh scan of what is loaded. */
    fun setNearby(counts: Map<SafariCritter, Int>) {
        nearbyCounts = counts
    }

    fun nearby(critter: SafariCritter) = nearbyCounts.getOrDefault(critter, 0)

    /**
     * How many of [critter] the run is considered to hold.
     *
     * Only a fixed quota can answer this. The client cannot see the whole map, and a partymate catching something
     * out of render distance is never observed at all, so nothing counted locally is a valid target.
     */
    fun required(critter: SafariCritter): Int = when {
        config.uniqueOnly -> 1
        critter.hasQuota -> critter.spawnQuota
        else -> 1
    }

    /** True when the run cannot produce [critter] and none was caught. */
    fun isUnavailable(critter: SafariCritter) = critter in unavailable && partyCatches(critter) == 0

    /**
     * True once the run is finished with [critter], either by catching enough or because the run can no longer
     * produce it. Treating the impossible as settled is what lets a biome read as complete instead of stalling
     * forever on a species that is never coming.
     */
    fun isComplete(critter: SafariCritter): Boolean {
        if (isUnavailable(critter)) return true
        return partyCatches(critter) >= required(critter)
    }

    fun remaining(critter: SafariCritter) = (required(critter) - partyCatches(critter)).coerceAtLeast(0)

    fun partyUnique() = SafariCritter.entries.count { isComplete(it) }

    fun partyUnique(biome: SafariBiome) = SafariCritter.inBiome(biome).count { isComplete(it) }

    fun partyTotal() = ownTotal() + sharedTotal()

    fun partyTotal(biome: SafariBiome) = ownTotal(biome) + sharedCatches.entries
        .filter { it.key.biome == biome }
        .sumOf { it.value.values.sum() }

    private fun sharedTotal() = sharedCatches.values.sumOf { it.values.sum() }

    fun partyCatches(critter: SafariCritter): Int =
        ownCatches.getOrDefault(critter, 0) + sharedCatches[critter]?.values?.sum().orZero()

    private fun Int?.orZero() = this ?: 0

    /** Who caught [critter] this run, local player included. */
    fun catchersOf(critter: SafariCritter): List<String> = buildList {
        if (caughtByYou(critter)) add(selfName)
        addAll(sharedCatches[critter]?.keys.orEmpty())
    }

    fun biomeComplete(biome: SafariBiome) = partyUnique(biome) == SafariCritter.totalIn(biome)

    /** True when every species except [exception] has been caught, whether or not the exception itself has been. */
    fun allCaughtExcept(exception: SafariCritter) = SafariCritter.entries.all { it == exception || isComplete(it) }

    fun dexComplete() = partyUnique() == SafariCritter.total

    /** Species in [biome] the run is not finished with yet. */
    fun missing(biome: SafariBiome) = SafariCritter.inBiome(biome).filter { !isComplete(it) }

    // --- per player breakdown ---

    /** Unique species count per player per biome, the local player first. */
    fun uniquePerPlayer(): Map<String, Map<SafariBiome, Int>> {
        val result = linkedMapOf<String, MutableMap<SafariBiome, Int>>()
        for (critter in ownCatches.keys) {
            result.getOrPut(selfName) { mutableMapOf() }.addOrPut(critter.biome, 1)
        }
        val others = sortedMapOf<String, MutableMap<SafariBiome, Int>>()
        for ((critter, byPlayer) in sharedCatches) {
            for (player in byPlayer.keys) {
                others.getOrPut(player) { mutableMapOf() }.addOrPut(critter.biome, 1)
            }
        }
        result.putAll(others)
        return result
    }

    fun players(): List<String> = uniquePerPlayer().keys.toList()

    // --- misc ---

    fun ownCatchCounts(): Map<SafariCritter, Int> = ownCatches.toMap()

    fun sharedCatchCounts(): Map<SafariCritter, Int> = sharedCatches.mapValues { it.value.values.sum() }

    fun attemptCounts(): Map<SafariCritter, Int> = attempts.toMap()

    fun attempts(critter: SafariCritter) = attempts.getOrDefault(critter, 0)

    fun failures(critter: SafariCritter) = failures.getOrDefault(critter, 0)

    fun totalAttempts() = attempts.values.sum()

    fun totalFailures() = failures.values.sum()

    /** Every shard that reached you this run, yours and loot shared alike. */
    fun totalShards() = ownShards + sharedShards

    /** How long the run has been going, for a live timer. */
    fun elapsed(): Duration = startTime.passedSince()

    /** The span of a finished run, start to last event. */
    fun duration(): Duration = lastEvent - startTime

    val lastEventTime get() = lastEvent

    /** True when nothing has been recorded yet, used to suppress an empty run. */
    fun isEmpty() = ownCatches.isEmpty() && sharedCatches.isEmpty() && attempts.isEmpty()
}
