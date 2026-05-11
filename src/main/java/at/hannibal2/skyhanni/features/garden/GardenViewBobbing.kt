package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft

@SkyHanniModule
object GardenViewBobbing {

    private val config get() = SkyHanniMod.feature.garden

    /** Vanilla preference before SkyHanni forced view bobbing off while on a garden island. */
    private var savedBobView: Boolean? = null

    private fun IslandType.isAnyGarden(): Boolean =
        this == IslandType.GARDEN || this == IslandType.GARDEN_GUEST

    private fun isOnGardenIsland(): Boolean =
        IslandType.GARDEN.isInIsland() || IslandType.GARDEN_GUEST.isInIsland()

    @HandleEvent(onlyOnSkyblock = true)
    fun onIslandChange(event: IslandChangeEvent) {
        if (!config.disableViewBobbingInGarden) {
            restoreIfActive()
            return
        }
        val wasGarden = event.oldIsland.isAnyGarden()
        val isGarden = event.newIsland.isAnyGarden()
        when {
            !wasGarden && isGarden -> disableForGarden()
            wasGarden && !isGarden -> restoreIfActive()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(20, 1)) return
        if (!config.disableViewBobbingInGarden) {
            restoreIfActive()
            return
        }
        if (isOnGardenIsland()) {
            disableForGarden()
            val options = Minecraft.getInstance().options
            if (savedBobView != null && options.bobView().get()) {
                options.bobView().set(false)
            }
        } else {
            restoreIfActive()
        }
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        restoreIfActive()
    }

    private fun disableForGarden() {
        if (savedBobView != null) return
        val options = Minecraft.getInstance().options
        savedBobView = options.bobView().get()
        options.bobView().set(false)
    }

    private fun restoreIfActive() {
        val previous = savedBobView ?: return
        Minecraft.getInstance().options.bobView().set(previous)
        savedBobView = null
    }
}
