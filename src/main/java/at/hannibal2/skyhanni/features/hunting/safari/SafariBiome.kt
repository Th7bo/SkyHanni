package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils

/**
 * The four biomes of the Critter Safari. Every critter species belongs to exactly one of them,
 * which is what makes "one biome per party member" a meaningful way to split a run.
 */
enum class SafariBiome(val displayName: String, val color: LorenzColor, colorCode: String) {
    FOREST("Forest", LorenzColor.GREEN, "§2"),
    CAVERN("Cavern", LorenzColor.GOLD, "§6"),
    ICY("Icy", LorenzColor.AQUA, "§9"),
    HAUNTED("Haunted", LorenzColor.DARK_PURPLE, "§5"),
    ;

    /** Name as the island graph and the scoreboard write it, e.g. `Forest Biome`. */
    val areaName = "$displayName Biome"

    val coloredName = color.getChatColor() + displayName

    val formattedName = "$colorCode$displayName"

    val waypointName = "$formattedName Biome"

    val shards: List<SafariShard> get() = SafariShard.entries.filter { it.biome == this }

    companion object {

        /** Resolves a bare biome name such as `Icy`, as this feature's own messages write it. */
        fun byDisplayName(name: String?): SafariBiome? =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }

        /** Resolves an area string such as `⏣ Icy Biome`. */
        fun byAreaName(area: String?): SafariBiome? =
            area?.let { name -> entries.firstOrNull { name.contains(it.areaName) } }

        fun currentArea(): SafariBiome? {
            val area = SkyBlockUtils.graphArea
            if (area == AreaNode.NO_AREA || area.isNullOrEmpty()) return null
            byAreaName(area)?.let { return it }
            ErrorManager.logErrorStateWithData(
                "Unknown Safari biome detected.",
                "Unknown Safari graph area.",
                "area" to area,
            )
            return null
        }
    }
}
