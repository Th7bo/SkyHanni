package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.milliseconds

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@SkyHanniModule
object CorpseFinder {

    private const val MARK_AS_FOUND_TICKS_THRESHOLD = 10

    /**
     * WRAPPED-REGEX-TEST: " Lapis: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Tungsten: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Umber: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Vanguard: NOT LOOTED"
     * WRAPPED-REGEX-TEST: " Lapis: LOOTED"
     * WRAPPED-REGEX-TEST: " Tungsten: LOOTED"
     * WRAPPED-REGEX-TEST: " Umber: LOOTED"
     * WRAPPED-REGEX-TEST: " Vanguard: LOOTED"
     */
    private val tabWidgetCorpsePattern by RepoPattern.pattern(
        "mining.glacitemineshaft.tabwidgetcorpse",
        "\\s*(?<corpse>\\w+): (?:NOT )?LOOTED\\s*",
    )

    // Map with the corpse entity as the key and the consecutive ticks count of passing canBeSeen checks as value
    private val corpseEntities = mutableMapOf<ArmorStand, Int>()
    private var totalCorpseCount = 0

    private fun areAllCorpsesFound(): Boolean {
        return totalCorpseCount > 0 &&
            totalCorpseCount == corpseEntities.size &&
            corpseEntities.all { it.value >= MARK_AS_FOUND_TICKS_THRESHOLD }
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!event.isHead || event.newItemStack == null) return
        if (!CorpseType.isValidHelmet(event.newItemStack.getInternalName())) return
        if (corpseEntities.any { it.key.uuid == event.entity.uuid }) return

        corpseEntities[event.entity] = 0
        scheduleThroughWallMark(event.entity)
    }

    /**
     * Fork customization: mark detected corpses through walls after a short random delay, without waiting for
     * line-of-sight (the [onPlayerMove] [canBeSeen] path). Reuses the upstream [CorpseFoundEvent] flow so the
     * waypoint + chat handling stays in [MineshaftWaypoints], and flags the entity as found so the line-of-sight
     * path skips it (no double-marking).
     */
    private fun scheduleThroughWallMark(entity: ArmorStand) {
        DelayedRun.runDelayed((500..3000).random().milliseconds) {
            val ticks = corpseEntities[entity] ?: return@runDelayed
            if (ticks >= MARK_AS_FOUND_TICKS_THRESHOLD) return@runDelayed
            val corpseType = CorpseType.fromEntityOrNull(entity) ?: return@runDelayed
            corpseEntities[entity] = MARK_AS_FOUND_TICKS_THRESHOLD
            CorpseFoundEvent(corpseType, entity.getLorenzVec().up(), areAllCorpsesFound()).post()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        for ((entity, canBeSeenTicks) in corpseEntities) {
            if (canBeSeenTicks >= MARK_AS_FOUND_TICKS_THRESHOLD) continue

            if (!entity.getLorenzVec().canBeSeen(-1..3)) {
                corpseEntities[entity] = 0
                continue
            }

            val corpseType = CorpseType.fromEntityOrNull(entity) ?: ErrorManager.skyHanniError(
                "Got CorpseType of null for entity in corpseEntities",
                "event" to "EntityMoveEvent<LocalPlayer>",
                "helmet" to entity.equipment.get(EquipmentSlot.HEAD).getInternalName(),
                "location" to entity.getLorenzVec(),
            )

            if (corpseEntities.addOrPut(entity, 1) >= MARK_AS_FOUND_TICKS_THRESHOLD) {
                CorpseFoundEvent(corpseType, entity.getLorenzVec().up(), areAllCorpsesFound()).post()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onEntityClick(event: EntityClickEvent) {
        val clickedEntityUuid = event.clickedEntity.uuid
        val (entity, canBeSeenTicks) = corpseEntities.entries.firstOrNull { it.key.uuid == clickedEntityUuid } ?: return

        if (canBeSeenTicks >= MARK_AS_FOUND_TICKS_THRESHOLD) return

        val corpseType = CorpseType.fromEntityOrNull(entity) ?: ErrorManager.skyHanniError(
            "Got CorpseType of null for entity in corpseEntities",
            "event" to "EntityClickEvent",
            "helmet" to entity.equipment.get(EquipmentSlot.HEAD).getInternalName(),
            "location" to entity.getLorenzVec(),
        )

        corpseEntities[entity] = MARK_AS_FOUND_TICKS_THRESHOLD
        CorpseFoundEvent(corpseType, entity.getLorenzVec().up(), areAllCorpsesFound()).post()
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (event.widget != TabWidget.FROZEN_CORPSES) return
        totalCorpseCount = event.lines.count { tabWidgetCorpsePattern.matches(it) }
    }

    @HandleEvent
    fun onWorldChange() {
        corpseEntities.clear()
        totalCorpseCount = 0
    }
}
