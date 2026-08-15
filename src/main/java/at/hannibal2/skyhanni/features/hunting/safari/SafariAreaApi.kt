package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import java.util.PriorityQueue

/**
 * The one place that answers "where is the player, and which biome is that".
 *
 * The island graph is the authority. Its area nodes name the four biomes, but only a node standing on an area border
 * carries that name, so every other node is assigned the area it is closest to *along the graph edges* - which is how
 * [at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend] answers it for the player.
 *
 * That search does not depend on the player, so it is run once per graph load for every node at once. A position then
 * only needs a nearest node lookup, which makes "which biome is this trade in" answerable for any point on the map,
 * not just for where the player is standing.
 */
@SkyHanniModule
object SafariAreaApi {

    /** Past this distance from every known node, a position is not on the mapped Safari. */
    private const val MAX_NODE_DISTANCE = 40.0

    /** How many answered positions are kept before the memo is thrown away wholesale. */
    private const val MAX_CACHE_ENTRIES = 4096

    private var nodeBiomes: Map<GraphNode, SafariBiome> = emptyMap()

    /** A wrapper, so "this position has no biome" is cached as an answer rather than as a miss. */
    private class Cached(val value: SafariBiome?)

    private val cache = mutableMapOf<LorenzVec, Cached>()

    val inSafari get() = IslandType.SAFARI.isInIsland()

    /**
     * The Critter Safari Entrance, which is a separate area over in Torrhus Canyon rather than the island itself.
     * Its name also carries "Safari", which is why matching on that alone put overlays up in the canyon.
     */
    val atEntrance: Boolean
        get() = IslandType.TORRHUS_CANYON.isInIsland() && SkyBlockUtils.scoreboardArea?.contains("Critter Safari") == true

    /** Whether the player is at the Safari at all, the entrance included. */
    val inSafariOrEntrance get() = inSafari || atEntrance

    /** The biome being stood in, or null when it cannot be determined. */
    val currentBiome: SafariBiome?
        get() {
            if (!inSafari) return null
            SafariBiome.byAreaName(SkyBlockUtils.graphArea)?.let { return it }
            return biomeAt(LocationUtils.playerLocation())
        }

    /**
     * The biome a position falls in, or null outside the mapped areas.
     *
     * Answered per block and memoised, since the marks ask it for every floor drop on every frame and the answer
     * cannot change for a position that has not moved.
     */
    fun biomeAt(location: LorenzVec): SafariBiome? {
        if (nodeBiomes.isEmpty()) return null
        val block = location.roundToBlock()
        cache[block]?.let { return it.value }
        val found = nearestNodeBiome(block)
        if (cache.size > MAX_CACHE_ENTRIES) cache.clear()
        cache[block] = Cached(found)
        return found
    }

    private fun nearestNodeBiome(location: LorenzVec): SafariBiome? {
        var best: SafariBiome? = null
        var bestDistance = MAX_NODE_DISTANCE * MAX_NODE_DISTANCE
        for ((node, biome) in nodeBiomes) {
            val distance = node.position.distanceSq(location)
            if (distance >= bestDistance) continue
            bestDistance = distance
            best = biome
        }
        return best
    }

    @HandleEvent(IslandGraphReloadEvent::class, onlyOnIsland = IslandType.SAFARI)
    private fun onIslandGraphReload() {
        buildBiomeMap()
    }

    /**
     * Assigns every node the biome of the graph nearest area node, in one multi source Dijkstra pass.
     *
     * Straight line distance is not good enough here: the Safari's biomes are not convex, Forest and Haunted
     * interleave, and the cave sections fold back over each other.
     */
    private fun buildBiomeMap() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val distances = mutableMapOf<GraphNode, Double>()
        val biomes = mutableMapOf<GraphNode, SafariBiome>()
        val queue = PriorityQueue<Pair<GraphNode, Double>>(compareBy { it.second })

        // The graph stores every edge once; walking outwards from the area nodes needs both directions of it.
        val adjacency = mutableMapOf<GraphNode, MutableMap<GraphNode, Double>>()
        for (node in graph) {
            for ((neighbour, weight) in node.neighbors) {
                adjacency.getOrPut(node) { mutableMapOf() }[neighbour] = weight
                adjacency.getOrPut(neighbour) { mutableMapOf() }[node] = weight
            }
        }

        for (node in graph) {
            val biome = SafariBiome.byAreaName(node.cleanName) ?: continue
            distances[node] = 0.0
            biomes[node] = biome
            queue.add(node to 0.0)
        }

        while (queue.isNotEmpty()) {
            val (node, distance) = queue.poll()
            if (distance > distances.getOrDefault(node, Double.MAX_VALUE)) continue
            val biome = biomes[node] ?: continue
            for ((neighbour, weight) in adjacency[node].orEmpty()) {
                val next = distance + weight
                if (next >= distances.getOrDefault(neighbour, Double.MAX_VALUE)) continue
                distances[neighbour] = next
                biomes[neighbour] = biome
                queue.add(neighbour to next)
            }
        }

        nodeBiomes = biomes
        cache.clear()
    }

    @HandleEvent
    private fun onIslandJoin(event: IslandJoinEvent) {
        if (event.island != IslandType.SAFARI) return
        if (nodeBiomes.isEmpty()) buildBiomeMap()
    }

    @HandleEvent
    private fun onIslandLeave(event: IslandLeaveEvent) {
        if (event.island != IslandType.SAFARI) return
        nodeBiomes = emptyMap()
        cache.clear()
    }
}
