package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.utils.LorenzColor

/**
 * The four biomes of the Critter Safari. Every critter species belongs to exactly one of them,
 * which is what makes "one biome per party member" a meaningful way to split a run.
 */
enum class SafariBiome(val displayName: String, val color: LorenzColor) {
    FOREST("Forest", LorenzColor.GREEN),
    CAVERN("Cavern", LorenzColor.GOLD),
    ICY("Icy", LorenzColor.AQUA),
    HAUNTED("Haunted", LorenzColor.DARK_PURPLE),
    ;

    /** Name as the island graph and the scoreboard write it, e.g. `Forest Biome`. */
    val areaName = "$displayName Biome"

    val coloredName = color.getChatColor() + displayName

    companion object {

        /** Resolves a bare biome name such as `Icy`, as this feature's own messages write it. */
        fun byDisplayName(name: String?): SafariBiome? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }

        /** Resolves an area string such as `⏣ Icy Biome`. */
        fun byAreaName(area: String?): SafariBiome? =
            area?.let { name -> entries.firstOrNull { name.contains(it.areaName) } }
    }
}
