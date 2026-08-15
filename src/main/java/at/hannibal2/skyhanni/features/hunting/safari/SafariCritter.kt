package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.features.hunting.safari.SafariBiome.CAVERN
import at.hannibal2.skyhanni.features.hunting.safari.SafariBiome.FOREST
import at.hannibal2.skyhanni.features.hunting.safari.SafariBiome.HAUNTED
import at.hannibal2.skyhanni.features.hunting.safari.SafariBiome.ICY
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzRarity.COMMON
import at.hannibal2.skyhanni.utils.LorenzRarity.EPIC
import at.hannibal2.skyhanni.utils.LorenzRarity.LEGENDARY
import at.hannibal2.skyhanni.utils.LorenzRarity.RARE
import at.hannibal2.skyhanni.utils.LorenzRarity.UNCOMMON
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

/**
 * The complete Critterdex roster (37 species), grouped by biome.
 *
 * Some species spawn a fixed number of times per run, so catching one is not the same as clearing them. Those carry
 * a [spawnQuota]; everything else respawns and has none. The quotas come from replayed runs where the per-run counts
 * pile up on a single value, while species without a quota show no such ceiling.
 *
 * The Macaw looks quota-like but has none: it is RNG, can come more than once, and is not guaranteed at all.
 */
enum class SafariCritter(
    val displayName: String,
    val biome: SafariBiome,
    val rarity: LorenzRarity,
    val spawnQuota: Int = 0,
) {
    // Forest (9)
    FOXTROT("Foxtrot", FOREST, COMMON),
    BLUEBIRD("Bluebird", FOREST, UNCOMMON),
    HONEYBUG("Honeybug", FOREST, UNCOMMON),
    TREEFROG("Treefrog", FOREST, UNCOMMON),
    WOODCHUCKER("Woodchucker", FOREST, UNCOMMON),
    FLUFFLING("Fluffling", FOREST, RARE),
    HIDEONFLOOR("Hideonfloor", FOREST, RARE),
    PARAKEET("Parakeet", FOREST, RARE),
    MACAW("Macaw", FOREST, LEGENDARY),

    // Cavern (9)
    CAVERNFISH("Cavernfish", CAVERN, COMMON),
    FLITTER("Flitter", CAVERN, COMMON),
    SHYWORM("Shyworm", CAVERN, COMMON),
    DRIFTLING("Driftling", CAVERN, UNCOMMON),
    CHUCKWALLA("Chuckwalla", CAVERN, RARE),
    ROCKMITE("Rockmite", CAVERN, RARE),
    SCRAPPY("Scrappy", CAVERN, RARE),
    SNOOZLE("Snoozle", CAVERN, RARE),
    GEMZIE("Gemzie", CAVERN, EPIC, spawnQuota = 3),

    // Icy (9)
    STRONGARM("Strongarm", ICY, COMMON),
    TEPID("Tepid", ICY, COMMON),
    POLARIS("Polaris", ICY, UNCOMMON),
    SHUDDERSQUID("Shuddersquid", ICY, UNCOMMON),
    BILLYGOAT("Billygoat", ICY, RARE),
    MANTIS_SHRIMP("Mantis Shrimp", ICY, RARE),
    NOZZLENOSE("Nozzlenose", ICY, RARE),
    TROODON("Troodon", ICY, RARE, spawnQuota = 3),
    WUMPA("Wumpa", ICY, LEGENDARY, spawnQuota = 1),

    // Haunted (10)
    AREITA("Areita", HAUNTED, UNCOMMON),
    BLOODBAT("Bloodbat", HAUNTED, UNCOMMON),
    DUPLICO("Duplico", HAUNTED, UNCOMMON),
    GAZER("Gazer", HAUNTED, UNCOMMON, spawnQuota = 4),
    LITTERBUG("Litterbug", HAUNTED, UNCOMMON),
    SOLSNATCHER("Solsnatcher", HAUNTED, UNCOMMON),
    GIMMIEGOLD("Gimmiegold", HAUNTED, RARE),
    HIDEONWALL("Hideonwall", HAUNTED, RARE),
    HIDEYHO("Hideyho", HAUNTED, RARE, spawnQuota = 1),
    DOOMSPIRAL("Doomspiral", HAUNTED, LEGENDARY, spawnQuota = 1),
    ;

    /** True when a fixed number spawn per run, so "all of them" is a meaningful target. */
    val hasQuota get() = spawnQuota > 0

    val coloredName = rarity.color.getChatColor() + displayName

    /**
     * The bazaar product this species' shard trades as.
     *
     * Derived rather than tabulated: every one of the 37 is `SHARD_` followed by the species name upper cased with
     * spaces underscored. A species Hypixel later names differently would simply have no price.
     */
    val shardInternalName: NeuInternalName = "SHARD_${displayName.uppercase().replace(' ', '_')}".toInternalName()

    companion object {

        val total = entries.size

        private val byDisplayName = entries.associateBy { it.displayName }

        /** Species ordered longest name first, so substring matching never mis-resolves. */
        private val byNameLengthDescending = entries.sortedByDescending { it.displayName.length }

        private val byBiome = entries.groupBy { it.biome }

        fun inBiome(biome: SafariBiome): List<SafariCritter> = byBiome[biome].orEmpty()

        fun totalIn(biome: SafariBiome): Int = inBiome(biome).size

        fun byName(name: String?): SafariCritter? = name?.let { byDisplayName[it] }

        /**
         * Finds the species named somewhere inside a chat line.
         *
         * Matching by roster lookup rather than by a full sentence regex means unseen message wordings still resolve
         * correctly as long as the species name appears verbatim.
         */
        fun findIn(line: String): SafariCritter? = byNameLengthDescending.firstOrNull { it.displayName in line }
    }
}
