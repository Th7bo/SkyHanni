package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

/**
 * The critters the client can currently see, scanned once per sweep.
 *
 * Hypixel labels every critter with an entity whose custom name is exactly the species name, sitting on top of the
 * mob rather than being it. So a sighting is two entities: the label that says what it is, and the body underneath
 * that says where it is and how big it is.
 *
 * Several features want this - the nearby spawn counter, the hard to find outlines, the Hideyho solver, the
 * sparkling call and the recatch helper - and each would otherwise sweep the entity list for itself.
 */
@SkyHanniModule
object SafariEntities {

    /** Spawns do not appear fast enough to be worth scanning every tick. */
    private const val SCAN_INTERVAL_TICKS = 5

    /** The label sits directly above its mob, so the pairing radius can be tight. */
    private const val LABEL_TO_MOB_RADIUS = 3.0

    /** The word a rare variant carries in its name. */
    private const val SPARKLING = "Sparkling"

    /** Hideyho arrives as a player rather than as a mob, so its skin is what names it. */
    private val hideyhoSkin by SkullTextureHolder.texture("HIDEYHO")

    /**
     * One labelled critter. [mob] is null when nothing was found under the label, which is the case for a Hideyho
     * (it arrives as a player) and for a capsule mid capture.
     */
    data class Sighting(
        val critter: SafariCritter,
        val label: Entity,
        val mob: LivingEntity?,
        val sparkling: Boolean,
    ) {
        /** The entity to point at: the mob if there is one, otherwise the label itself. */
        val body: Entity get() = mob ?: label
    }

    var sightings: List<Sighting> = emptyList()
        private set

    /**
     * When the list was last rebuilt. Between sweeps [sightings] holds the previous answer, so anything keeping its
     * own timestamps has to key off this rather than off the tick it read them on.
     */
    var lastScan: SimpleTimeMark = SimpleTimeMark.farPast()
        private set

    /**
     * Where Hideyho is standing, found by its skin rather than by a nametag.
     *
     * Hiding takes the nametag away but not the entity, so the sweep above cannot see a hidden one at all. The skin
     * is what is left to go on, and it is the same evidence
     * [at.hannibal2.skyhanni.features.hunting.safari.HideyhoFinder] confirms a hiding spot with.
     */
    var hideyhoLocation: LorenzVec? = null
        private set

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(SCAN_INTERVAL_TICKS)) return
        sightings = scan()
        lastScan = SimpleTimeMark.now()
        countNearby()
    }

    /**
     * Tallies what is loaded right now, by species.
     *
     * Replaced wholesale rather than merged, so anything caught or despawned since the last scan simply drops out.
     * It is not a total and cannot be: the client never sees the far side of the map. What it is good for is
     * "there is one of these next to you that nobody has caught".
     */
    private fun countNearby() {
        if (!SkyHanniMod.feature.hunting.safari.runTracker.countSpawns) return
        val session = SafariRunManager.currentSession ?: return
        session.setNearby(sightings.groupingBy { it.critter }.eachCount())
    }

    @OptIn(AllEntitiesGetter::class)
    private fun scan(): List<Sighting> {
        val labels = mutableListOf<Triple<Entity, SafariCritter, Boolean>>()
        val candidates = mutableListOf<LivingEntity>()
        var hideyho: LorenzVec? = null

        for (entity in EntityUtils.getAllEntities()) {
            if (entity is RemotePlayer && entity.isHideyho()) hideyho = entity.getLorenzVec()

            val named = resolve(entity)
            if (named != null) {
                labels.add(Triple(entity, named.first, named.second))
            } else if (entity is LivingEntity && entity !is ArmorStand && entity !is Player) {
                candidates.add(entity)
            }
        }

        hideyhoLocation = hideyho
        return labels.map { (label, critter, sparkling) ->
            Sighting(critter, label, nearestMob(candidates, label), sparkling)
        }
    }

    private fun RemotePlayer.isHideyho(): Boolean {
        val texture = hideyhoSkin ?: return false
        return getSkinTexture() == texture
    }

    /**
     * The species a label names, and whether it is the sparkling variant.
     *
     * Only the one `Sparkling` prefix is tolerated rather than searching the label for any species name: an armor
     * stand reading "Tepid Shard" must not count as a Tepid.
     */
    private fun resolve(entity: Entity): Pair<SafariCritter, Boolean>? {
        val name = entity.cleanName.takeIf { it.isNotEmpty() } ?: return null
        SafariCritter.byName(name)?.let { return it to false }
        if (!name.startsWith(SPARKLING, ignoreCase = true)) return null
        val critter = SafariCritter.byName(name.substring(SPARKLING.length).trim()) ?: return null
        return critter to true
    }

    private fun nearestMob(candidates: List<LivingEntity>, label: Entity): LivingEntity? {
        val position = label.getLorenzVec()
        return candidates
            .filter { it.distanceTo(position) < LABEL_TO_MOB_RADIUS }
            .minByOrNull { it.distanceTo(position) }
    }

    @HandleEvent
    private fun onWorldChange() {
        sightings = emptyList()
    }
}
