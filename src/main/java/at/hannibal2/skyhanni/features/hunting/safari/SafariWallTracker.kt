package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.world.level.block.Blocks

/**
 * The breakable walls that hide critters behind them.
 *
 * There are two sets, each at fixed positions in one biome: the Snooper walls in the Cavern and the Troodon walls in
 * the Icy biome. Both want breaking every run to check behind them. Being blocks rather than entities they are easy
 * to read: a wall is still standing exactly when its position is not air.
 *
 * The one trap is that an unloaded chunk also reports air, which would mark every distant wall as already broken.
 * [State.UNKNOWN] separates "broken" from "cannot see from here", and the two are shown differently.
 */
class SafariWallTracker private constructor(
    val displayName: String,
    val biome: SafariBiome,
    private val positions: List<LorenzVec>,
) {

    enum class State {
        /** Still standing - the position holds a block. */
        INTACT,

        /** Already broken this run. */
        BROKEN,

        /** Chunk not loaded, so air here would mean nothing. */
        UNKNOWN,
    }

    data class Wall(val position: LorenzVec, val state: State, val distance: Double)

    /** Every tracked wall with its current state, nearest first. */
    fun walls(): List<Wall> {
        if (MinecraftCompat.localWorldOrNull == null) return emptyList()
        return positions.map { position ->
            val state = when {
                !position.isInLoadedChunk() -> State.UNKNOWN
                position.getBlockAt() == Blocks.AIR -> State.BROKEN
                else -> State.INTACT
            }
            Wall(position, state, position.distanceToPlayer())
        }.sortedBy { it.distance }
    }

    /** Walls known to still be standing. Unknown ones are not counted either way. */
    fun intactCount() = walls().count { it.state == State.INTACT }

    fun unknownCount() = walls().count { it.state == State.UNKNOWN }

    /**
     * True only when every wall has been *confirmed* broken.
     *
     * A wall in an unloaded chunk does not count: air and out of range look identical from here, and concluding
     * "all broken" from chunks nobody has visited would be exactly backwards.
     */
    fun allConfirmedBroken(): Boolean {
        val walls = walls()
        return walls.isNotEmpty() && walls.all { it.state == State.BROKEN }
    }

    companion object {
        /** The Cavern walls. Snoozle comes from behind one of these, if it comes at all. */
        val SNOOPER = SafariWallTracker(
            "Snooper",
            SafariBiome.CAVERN,
            listOf(
                LorenzVec(-126, 39, 74),
                LorenzVec(-114, 39, 87),
                LorenzVec(-70, 39, 68),
                LorenzVec(-96, 40, 17),
                LorenzVec(-95, 40, 42),
            ),
        )

        /** The Icy walls. */
        val TROODON = SafariWallTracker(
            "Troodon",
            SafariBiome.ICY,
            listOf(
                LorenzVec(-104, 80, -95),
                LorenzVec(-131, 78, -61),
                LorenzVec(-109, 89, -27),
            ),
        )

        val all = listOf(SNOOPER, TROODON)
    }
}
