package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType.SAFARI
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object SparklingNotifier {

    private val config get() = SkyHanniMod.feature.hunting.safari.sparklingNotifier
    private val partyConfig get() = SkyHanniMod.feature.hunting.safari.party

    // Hypixel marks these mobs with an all caps keyword in their nametag.
    private const val SPARKLING_KEYWORD = "SPARKLING"

    // How far below its nametag the mob itself is looked for.
    private const val NAMETAG_SEARCH_RADIUS = 2.0

    /**
     * Detection reads the nametag armor stand directly instead of going through mob detection. The Critter Safari
     * has no [at.hannibal2.skyhanni.data.mob.IslandExceptions] entry, so its mobs fall back to
     * [at.hannibal2.skyhanni.data.mob.MobFactories.basic], whose filter requires a trailing health value that these
     * nametags do not appear to have. No Mob would be built, and no MobEvent would ever fire.
     *
     * Maps each sparkling nametag to the mob standing under it, which is null while the mob has not loaded yet.
     */
    private val sparklingNametags = mutableMapOf<ArmorStand, LivingEntity?>()
    private val notifiedEntityIds = mutableSetOf<Int>()

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onEntityCustomNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        checkNametag(event.entity)
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onEntityEnterWorld(event: EntityEnterWorldEvent<Entity>) {
        checkNametag(event.entity as? ArmorStand ?: return)
    }

    private fun checkNametag(nametag: ArmorStand) {
        if (!config.enabled) return
        if (SPARKLING_KEYWORD !in nametag.cleanName) return
        if (nametag in sparklingNametags) return

        sparklingNametags[nametag] = nametag.findMobBelow()

        // The nametag is re-sent whenever it comes back into render distance, so notify only once per entity.
        if (!notifiedEntityIds.add(nametag.id)) return

        val name = nametag.sparklingName
        val where = describe(nametag.getLorenzVec().roundToBlock())
        TitleManager.sendTitle("§d§lSparkling $name!", where)
        ChatUtils.notifyOrDisable("Found a Sparkling $name! ($where)", config::enabled)
        // Biome and coordinates, so a partymate covering another biome can be sent straight to it.
        SafariEncounterAlerts.post(partyConfig.sparkling, "SPARKLING $name! ($where)")
    }

    private fun describe(position: LorenzVec): String {
        val coords = "${position.x.toInt()} ${position.y.toInt()} ${position.z.toInt()}"
        return SafariAreaApi.biomeAt(position)?.let { "${it.displayName} $coords" } ?: coords
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onSecondPassed() {
        sparklingNametags.removeIf { (nametag, _) ->
            !nametag.isAlive || SPARKLING_KEYWORD !in nametag.cleanName
        }
        // Retries mobs that had not loaded yet when their nametag showed up.
        for (entry in sparklingNametags.entries) {
            if (entry.value?.isAlive != true) entry.setValue(entry.key.findMobBelow())
        }
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        for (mob in sparklingNametags.values) {
            val boundingBox = mob?.boundingBox ?: continue
            event.drawFilledBoundingBox(boundingBox, config.color, seeThroughBlocks = true)
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        sparklingNametags.clear()
        notifiedEntityIds.clear()
    }

    private fun ArmorStand.findMobBelow(): LivingEntity? = getLorenzVec()
        .getEntitiesNearby<LivingEntity>(NAMETAG_SEARCH_RADIUS)
        .filter { it !is ArmorStand }
        .minByOrNull { it.distanceTo(this) }

    // Drops the mob type icons in front of the keyword along with the keyword itself, leaving just the mob name.
    private val ArmorStand.sparklingName: String
        get() = cleanName.substringAfter(SPARKLING_KEYWORD).trim().ifEmpty { "Mob" }
}
