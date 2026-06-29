package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkyBlockXPApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.StringUtils.lastColorCode
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.player.Player

@SkyHanniModule
object PlayerLevelNametagColor {
    private val config get() = SkyHanniMod.feature.gui.skyBlockLevelColors

    /**
     * REGEX-TEST: §8[§b480§8] §6Player
     * REGEX-TEST: §8[§6419§8] §bPlayer
     */
    private val levelPattern by RepoPattern.pattern(
        "misc.nametag.level",
        "\\[(?:§.)*(?<level>[\\d,]+)(?:§.)*]",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderNametag(event: EntityDisplayNameEvent<Player>) {
        if (!config.colorInNametags) return

        val text = event.chatComponent.formattedTextCompat()
        val recolored = levelPattern.findMatcher(text) {
            val levelText = group("level").removeColor()
            val level = levelText.formatIntOrNull() ?: return
            val before = text.substring(0, start())
            // The name after the bracket is a separate component now, so it no longer inherits the legacy
            // color codes from before the bracket. Re-apply the active color so the name keeps its color.
            val carriedColor = before.lastColorCode().orEmpty()
            before.asComponent()
                .append("§8[")
                .append(SkyBlockXPApi.getLevelColorComponent(level, levelText))
                .append("§8]")
                .append(carriedColor + text.substring(end()))
        } ?: return

        event.chatComponent = recolored
    }
}
