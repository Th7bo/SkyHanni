package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariHighlightConfig.MarkStyle
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.world.phys.AABB

/**
 * The one place that decides what gets a mark in the world, and in which biome.
 *
 * Everything but the trades and the recatch pin belongs to one biome, so it is only marked while you are standing in
 * that biome: boxes floating through the terrain of a biome they have nothing to do with are just noise.
 */
@SkyHanniModule
object SafariHighlights {

    private val config get() = SkyHanniMod.feature.hunting.safari.highlights

    /**
     * Species awkward enough to spot that a box around them earns its place. Bloodbats are small, dark, and sit
     * against Haunted geometry the same color as they are.
     */
    private val hardToFind = setOf(SafariCritter.BLOODBAT)

    /**
     * The perches a Hideonwall can turn up on.
     *
     * Unlike everything else here these are not read from the world - nothing about a perch looks different when it
     * is empty. They are the eight places one has been seen, so the mark says "check here", not "one is here".
     */
    private val hideonwallPerches = listOf(
        LorenzVec(18, 71, -57),
        LorenzVec(-5, 71, -85),
        LorenzVec(-23, 71, -76),
        LorenzVec(-25, 71, -65),
        LorenzVec(-25, 79, -57),
        LorenzVec(6, 79, -84),
        LorenzVec(17, 80, -75),
        LorenzVec(17, 79, -52),
    )

    /**
     * One thing worth walking to. A box rather than a position, because most of these are blocks and so are exactly
     * a block big, but a pinned critter is whatever size that critter is.
     */
    private data class Marker(
        val box: AABB?,
        val block: LorenzVec?,
        val label: String,
        val color: ChromaColour,
        val style: MarkStyle,
    )

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (marker in collect()) {
            val seeThrough = marker.style == MarkStyle.WAYPOINT
            val center = when {
                marker.block != null -> {
                    event.drawWaypointFilled(marker.block, marker.color.toColor(), seeThroughBlocks = seeThrough)
                    marker.block.add(0.5, 1.0, 0.5)
                }
                marker.box != null -> {
                    event.drawFilledBoundingBox(marker.box, marker.color, seeThroughBlocks = seeThrough)
                    LorenzVec(
                        (marker.box.minX + marker.box.maxX) / 2,
                        marker.box.maxY + 0.3,
                        (marker.box.minZ + marker.box.maxZ) / 2,
                    )
                }
                else -> continue
            }
            if (marker.style != MarkStyle.WAYPOINT) continue
            val distance = center.distanceToPlayer().toInt()
            event.drawDynamicText(center, "§e${marker.label} §7${distance}m", 0.8)
        }
    }

    private fun collect(): List<Marker> = buildList {
        val biome = SafariAreaApi.currentBiome
        addFixedSpots(biome)
        addSpottedThings(biome)
        addLiveMarks(biome)
    }

    /** The things that live at known positions, and the walls that are read off known positions. */
    private fun MutableList<Marker>.addFixedSpots(biome: SafariBiome?) {
        val colors = config.colors

        if (config.snooperWalls) addWalls(SafariWallTracker.SNOOPER, biome, colors.snooperWalls)
        if (config.troodonWalls) addWalls(SafariWallTracker.TROODON, biome, colors.troodonWalls)

        if (config.hideonwallPerches && biome == SafariBiome.HAUNTED) {
            for (perch in hideonwallPerches) {
                add(block(perch, "Hideonwall perch", colors.hideonwallPerches))
            }
        }
    }

    /** The things found by sweeping the world: nests, mounds and floor drops. */
    private fun MutableList<Marker>.addSpottedThings(biome: SafariBiome?) {
        val colors = config.colors

        if (config.nests && biome == SafariBiome.FOREST) {
            for (nest in SafariNestTracker.nests()) {
                if (!nest.unpunched) continue
                add(block(nest.position, "Nest", colors.nests))
            }
        }

        if (config.mounds && biome == SafariBiome.CAVERN) {
            for (mound in SafariMoundTracker.mounds()) {
                add(block(mound, "Mound", colors.mounds))
            }
        }

        // Drops turn up in every biome, so each one is gated on its own position rather than on the feature: else
        // standing near a border would mark drops on the far side of it.
        val dropStyle = config.floorDrops
        if (dropStyle == MarkStyle.OFF || biome == null) return
        for (drop in SafariFloorDrops.positions) {
            if (SafariAreaApi.biomeAt(drop) != biome) continue
            add(Marker(null, drop, "Floor drop", colors.floorDrops, dropStyle))
        }
    }

    /** The marks that belong to something happening right now: trades, Hideyho, critters and the recatch pin. */
    private fun MutableList<Marker>.addLiveMarks(biome: SafariBiome?) {
        val colors = config.colors

        // Not gated on the biome: a trade is worth crossing the map for, which is the whole point of marking it.
        if (config.trades) {
            for (trade in SafariTraderTracker.found) {
                val spot = trade.spot ?: continue
                add(block(spot.position, trade.critter.displayName, colors.trades))
            }
        }

        // Said plainly when the mark is a memory rather than a sighting: it is where it was when the client last
        // had it, which is where it still is unless it has re-hidden.
        if (config.hideyhoSolver && biome == SafariBiome.HAUNTED) {
            SafariHideyhoSolver.position?.let { position ->
                val label = if (SafariHideyhoSolver.live) "Hideyho" else "Hideyho (last seen)"
                add(block(position, label, colors.hideyho))
            }
        }

        // Boxed where it stands, never through the terrain: this marks something you could see anyway, which is the
        // difference between helping you spot it and telling you what is behind a wall.
        if (config.hardToFind) {
            for (sighting in SafariEntities.sightings) {
                if (sighting.critter !in hardToFind) continue
                val mob = sighting.mob ?: continue
                val name = sighting.critter.displayName
                add(Marker(mob.boundingBox, null, name, colors.hardToFind, MarkStyle.HIGHLIGHT))
            }
        }

        // Not gated on the biome either: it is pinned by a capsule you threw, so it is wherever you were standing.
        if (!config.recatchHelper) return
        val box = SafariRecatchHelper.pinnedBox ?: return
        val critter = SafariRecatchHelper.pinnedCritter ?: return
        add(Marker(box, null, critter.displayName, colors.recatch, MarkStyle.WAYPOINT))
    }

    /**
     * Marks the walls of one set that are still standing. A broken wall leaves air behind, and air is also what an
     * unloaded chunk reports, so only walls confirmed to still hold a block are marked.
     */
    private fun MutableList<Marker>.addWalls(
        walls: SafariWallTracker,
        biome: SafariBiome?,
        color: ChromaColour,
    ) {
        if (biome != walls.biome) return
        for (wall in walls.walls()) {
            if (wall.state != SafariWallTracker.State.INTACT) continue
            add(block(wall.position, "${walls.displayName} wall", color))
        }
    }

    private fun block(position: LorenzVec, label: String, color: ChromaColour) =
        Marker(null, position, label, color, MarkStyle.WAYPOINT)
}
