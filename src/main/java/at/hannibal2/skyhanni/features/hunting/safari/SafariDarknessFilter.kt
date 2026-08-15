package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.world.effect.MobEffects

/**
 * Drops the Warden darkness effect while at the Safari.
 *
 * Everything that renders the darkness - the light texture, the fog - reads the effect off the local player, so
 * taking the instance off the client's copy is enough. Nothing is sent to the server; this is the client choosing
 * not to draw something it was told about, and a re-applied effect is simply dropped again on the next tick.
 */
@SkyHanniModule
object SafariDarknessFilter {

    private val config get() = SkyHanniMod.feature.hunting.safari

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onTick() {
        if (!config.removeDarkness) return
        val player = MinecraftCompat.localPlayerOrNull ?: return
        if (!player.hasEffect(MobEffects.DARKNESS)) return
        player.removeEffect(MobEffects.DARKNESS)
    }
}
