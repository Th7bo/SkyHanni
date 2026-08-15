package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockUtils

/**
 * What is still uncaught **in the biome you are standing in** - the working list for whoever is assigned it.
 *
 * Species already caught by a partymate count as done, since the run's goal is party wide coverage. Under the list
 * come the things that are not critters but are still owed: walls to break, nests to punch, mounds to smash.
 */
@SkyHanniModule
object SafariMissingDisplay {

    private val config get() = SkyHanniMod.feature.hunting.safari.missing
    private val trackerConfig get() = SkyHanniMod.feature.hunting.safari.runTracker

    private var display = emptyList<String>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onSecondPassed() {
        display = if (isEnabled()) buildDisplay() else emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderStrings(display, posLabel = "Safari Missing List")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && SafariDisplayUtils.showsHere()

    private fun buildDisplay(): List<String> {
        val biome = SafariAreaApi.currentBiome ?: return emptyList()
        val session = SafariRunManager.currentOrLast
        return buildList {
            addCritters(biome, session)
            addWalls(biome, SafariWallTracker.SNOOPER, config.showSnooperWalls)
            addWalls(biome, SafariWallTracker.TROODON, config.showTroodonWalls)
            addNests(biome)
            addMounds(biome)
        }
    }

    private fun MutableList<String>.addCritters(biome: SafariBiome, session: SafariRunSession?) {
        // Before the first catch there is no session yet, but standing in a biome with nothing caught is exactly
        // when the full list is most useful, so fall back to the whole roster rather than hiding the display.
        val missing = session?.missing(biome) ?: SafariCritter.inBiome(biome)

        // A species already accounted for still deserves listing while one of them is stood in front of you: every
        // catch is another shard, so "done" only ever meant done for the dex.
        val alsoHere = if (session == null) emptyList() else SafariCritter.inBiome(biome)
            .filter { it !in missing && session.nearby(it) > 0 }

        if (missing.isEmpty()) {
            add("${biome.coloredName} Biome §a- all ${SafariCritter.totalIn(biome)} caught")
        } else {
            add("${biome.coloredName} Biome §7- ${missing.size} left")
            for (critter in missing) {
                add("${critter.coloredName} §8${note(session, critter)}")
            }
        }

        // Anything the run can no longer produce is stated outright rather than just dropping off the list, so its
        // absence does not look like a tracking bug.
        session?.let {
            for (critter in SafariCritter.inBiome(biome).filter(it::isUnavailable)) {
                add("§8${critter.displayName} none this run")
            }
        }

        // Listed after the outstanding ones, dimmer, so they read as a bonus rather than as something still owed.
        if (alsoHere.isNotEmpty() && session != null) {
            add("§8also here")
            for (critter in alsoHere) {
                add("§a${critter.displayName} §8${session.nearby(critter)} near")
            }
        }
    }

    /**
     * Nearby beats everything: it means one is in front of you right now. Then quota progress, then an attempt
     * count, which means it keeps escaping.
     */
    private fun note(session: SafariRunSession?, critter: SafariCritter): String {
        if (session == null) return ""
        val near = session.nearby(critter)
        if (near > 0) return "$near near"
        val required = session.required(critter)
        if (required > 1 && !trackerConfig.uniqueOnly) return "${session.partyCatches(critter)}/$required"
        val attempts = session.attempts(critter)
        return if (attempts > 0) "$attempts tried" else ""
    }

    /**
     * Counts one set of walls still to break, while you are in the biome they are in. A wall in an unloaded chunk
     * is added as `+n?` rather than counted: air and out of range look identical from here.
     */
    private fun MutableList<String>.addWalls(biome: SafariBiome, walls: SafariWallTracker, show: Boolean) {
        if (biome != walls.biome || !show) return
        val intact = walls.intactCount()
        val unknown = walls.unknownCount()
        if (intact == 0 && unknown == 0) {
            add("§a${walls.displayName} walls all broken")
            return
        }
        val unknownText = if (unknown > 0) " §8(+$unknown?)" else ""
        add("§6${walls.displayName} walls to break: §f$intact$unknownText")
    }

    private fun MutableList<String>.addNests(biome: SafariBiome) {
        if (biome != SafariBiome.FOREST || !config.showNests) return
        val nests = SafariNestTracker.nests()
        if (nests.isEmpty()) return
        val unpunched = nests.count { it.unpunched }
        // Just the count: the highlights know where they are.
        if (unpunched == 0) add("§aall ${nests.size} nests punched")
        else add("§6Bee nests to punch: §f$unpunched")
    }

    private fun MutableList<String>.addMounds(biome: SafariBiome) {
        if (biome != SafariBiome.CAVERN) return

        if (config.showMoundCount) {
            // Mounds are found by their hitbox within scanning range, so a zero would read as "none left" when it
            // only means "none near you".
            val nearby = SafariMoundTracker.mounds().size
            if (nearby > 0) add("§6Mounds to break: §f$nearby")
        }

        if (!config.showMoundsBroken || SafariMoundTracker.broken == 0) return
        add("§6Mounds broken: §f${SafariMoundTracker.broken} §8→ ${SafariMoundTracker.rockmites} Rockmite")
    }
}
