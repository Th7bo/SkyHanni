package at.hannibal2.skyhanni.features.hunting.safari

import com.google.gson.annotations.Expose

/**
 * A finished run, flattened to what is worth keeping.
 *
 * Species are stored by name, so a saved run survives the roster being edited: an unknown name is simply skipped
 * when it is read back. What is dropped is everything that only matters live - who caught what (only the counts
 * survive), what was loaded nearby, and the sparkling flags.
 */
class SafariRunRecord {

    @Expose
    var started: Long = 0

    @Expose
    var ended: Long = 0

    @Expose
    var self: String = ""

    /** Species name -> times you caught it. */
    @Expose
    var own: MutableMap<String, Int> = mutableMapOf()

    /** Species name -> times a partymate caught it, summed across the party. */
    @Expose
    var shared: MutableMap<String, Int> = mutableMapOf()

    /** Species name -> capsules thrown at it. */
    @Expose
    var attempts: MutableMap<String, Int> = mutableMapOf()

    /** Species name -> shards that reached you, yours and loot shared together. */
    @Expose
    var shards: MutableMap<String, Int> = mutableMapOf()

    @Expose
    var ownShards: Int = 0

    @Expose
    var totalShards: Int = 0

    val durationMillis get() = (ended - started).coerceAtLeast(0)

    /** Whether this run knows which species its shards came from, and so can be priced. */
    fun hasShardData() = shards.isNotEmpty()

    fun shards(critter: SafariCritter) = shards.getOrDefault(critter.displayName, 0)

    /** How many times anyone in the party caught [critter] in this run. */
    fun caught(critter: SafariCritter) =
        own.getOrDefault(critter.displayName, 0) + shared.getOrDefault(critter.displayName, 0)

    fun partyUnique() = SafariCritter.entries.count { caught(it) > 0 }

    fun ownUnique() = SafariCritter.entries.count { own.getOrDefault(it.displayName, 0) > 0 }

    fun partyTotal() = own.values.sum() + shared.values.sum()

    fun ownTotal() = own.values.sum()

    companion object {
        fun of(session: SafariRunSession): SafariRunRecord = SafariRunRecord().apply {
            started = session.startTime.toMillis()
            ended = session.lastEventTime.toMillis()
            self = session.selfName
            session.ownCatchCounts().forEach { (critter, count) -> own[critter.displayName] = count }
            session.sharedCatchCounts().forEach { (critter, count) -> shared[critter.displayName] = count }
            session.attemptCounts().forEach { (critter, count) -> attempts[critter.displayName] = count }
            session.shards.forEach { (critter, count) -> shards[critter.displayName] = count }
            ownShards = session.ownShards
            totalShards = session.totalShards()
        }
    }
}
