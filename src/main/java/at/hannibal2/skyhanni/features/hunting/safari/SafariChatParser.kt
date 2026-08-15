package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class SafariEventType {
    /** `CAPTURE! You caught a Foxtrot ...` - a catch credited to the local player. */
    OWN_CATCH,

    /** `LOOT SHARE! ... from <player> catching a Foxtrot!` - a partymate's catch. */
    SHARED_CATCH,

    /** `You threw a Critter Capsule at the Foxtrot!` */
    ATTEMPT,

    /** `The Foxtrot escaped your Critter Capsule!` or dodged it. */
    FAILED,
    ;

    /** True for the two event types that add to a catch tally. */
    val isCatch get() = this == OWN_CATCH || this == SHARED_CATCH
}

data class SafariCatchEvent(
    val type: SafariEventType,
    val critter: SafariCritter,
    val catcher: String? = null,
    val shards: Int = 0,
    val sparkling: Boolean = false,
)

/**
 * Turns a Critter Safari chat line into a [SafariCatchEvent].
 *
 * Rather than pin one pattern per wording, the parser keys off the `CAPTURE!` / `LOOT SHARE!` prefix and then
 * resolves the species by roster lookup. Unseen wordings, notably the self catch form of a SPARKLING, still parse
 * correctly as long as the species name is present.
 */
object SafariChatParser {

    private val patternGroup = RepoPattern.group("hunting.safari.catches")

    /**
     * REGEX-TEST: CAPTURE! You caught a Foxtrot and gained a Foxtrot Shard!
     * REGEX-TEST: CAPTURE! You found the Hideyho, and as a reward it gave you 3x Hideyho Shard!
     * REGEX-TEST: CAPTURE! You caught a Gemzie and gained 2x Gemzie Shard!
     */
    private val capturePattern by patternGroup.pattern(
        "own",
        "CAPTURE! (?<message>.*)",
    )

    /**
     * REGEX-TEST: LOOT SHARE! You received a Foxtrot Shard from Jaegerss catching a Foxtrot!
     * REGEX-TEST: LOOT SHARE! You received 3x Gemzie Shard from Th7bo catching a Gemzie!
     * REGEX-TEST: LOOT SHARE! You received a Hideyho Shard from Th7bo finding Hideyho!
     * REGEX-TEST: LOOT SHARE! You received a Rainbow Feather and 2x Rockmite Shard from Th7bo catching a SPARKLING Rockmite!
     */
    private val lootSharePattern by patternGroup.pattern(
        "shared",
        "LOOT SHARE! (?<message>.*from (?<catcher>\\w{1,16}) (?:catching|finding).*)",
    )

    /**
     * REGEX-TEST: You threw a Critter Capsule at the Rockmite!
     * REGEX-TEST: You threw a Critter Capsule at the Mantis Shrimp!
     */
    private val attemptPattern by patternGroup.pattern(
        "attempt",
        "You threw a Critter Capsule at the (?<critter>.+)!",
    )

    /**
     * REGEX-TEST: The Rockmite escaped your Critter Capsule!
     * REGEX-TEST: The Foxtrot dodged your critter capsule!
     */
    private val failedPattern by patternGroup.pattern(
        "failed",
        "The (?<critter>.+?) (?:escaped your Critter Capsule|dodged your critter capsule).*",
    )

    /**
     * REGEX-TEST: You caught a Gemzie and gained 2x Gemzie Shard!
     * REGEX-TEST: You received 1,024x Foxtrot Shard from Th7bo catching a Foxtrot!
     */
    private val shardAmountPattern by patternGroup.pattern(
        "shard-amount",
        ".*?(?<amount>\\d[\\d,]*)x\\s+\\S.*",
    )

    private const val SPARKLING_KEYWORD = "SPARKLING"

    fun parse(message: String): SafariCatchEvent? {
        capturePattern.matchMatcher(message) {
            val critter = SafariCritter.findIn(message) ?: return null
            return SafariCatchEvent(
                SafariEventType.OWN_CATCH,
                critter,
                shards = shardAmount(message),
                sparkling = SPARKLING_KEYWORD in message,
            )
        }

        lootSharePattern.matchMatcher(message) {
            val critter = SafariCritter.findIn(message) ?: return null
            return SafariCatchEvent(
                SafariEventType.SHARED_CATCH,
                critter,
                catcher = group("catcher"),
                shards = shardAmount(message),
                sparkling = SPARKLING_KEYWORD in message,
            )
        }

        attemptPattern.matchMatcher(message) {
            val critter = SafariCritter.byName(group("critter")) ?: return null
            return SafariCatchEvent(SafariEventType.ATTEMPT, critter)
        }

        failedPattern.matchMatcher(message) {
            val critter = SafariCritter.byName(group("critter")) ?: return null
            return SafariCatchEvent(SafariEventType.FAILED, critter)
        }

        return null
    }

    /** `gained 2x Foxtrot Shard` yields 2; `gained a Foxtrot Shard` has no numeral and yields 1. */
    private fun shardAmount(message: String): Int {
        shardAmountPattern.matchMatcher(message) {
            return group("amount").replace(",", "").toIntOrNull() ?: 1
        }
        return 1
    }
}
