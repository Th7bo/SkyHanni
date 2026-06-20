package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SkyBlockXPApi {

    private val group = RepoPattern.group("skyblockxpapi.inventory")

    private val itemNamePattern by group.pattern("itemname", "§aSkyBlock Leveling")

    /**
     * REGEX-TEST: §7Your SkyBlock Level: §8[§9287§8]
     */
    private val levelPattern by group.pattern("level", "§7Your SkyBlock Level: §8\\[§.(?<level>\\d+)§8\\]")

    /**
     * REGEX-TEST: §3§l§m      §f§l§m                   §r §b24§3/§b100 §bXP
     */
    private val xpPattern by group.pattern("xp", "[§\\w\\s]+§b(?<xp>\\d+)§3\\/§b100 §bXP")

    val levelXPPair get() = storage?.toLevelXPPair()

    // Stored as 12345, 123 is the level, 45 is the xp
    private var storage
        get() = ProfileStorageData.profileSpecific?.totalSkyBlockXP
        set(value) {
            ProfileStorageData.profileSpecific?.totalSkyBlockXP = value
        }

    private fun Int.toLevelXPPair() = this / 100 to this % 100

    /** Converts a 6-digit hex string (e.g. `"FF5C5C"`) to Minecraft's legacy hex format `§x§F§F§5§C§5§C`. */
    private fun hexColor(hex: String): String = "§x" + hex.uppercase().map { "§$it" }.joinToString("")

    // The default (vanilla) palette: one named Minecraft color per 40 levels, capped at dark red.
    private val defaultLevelColors: Map<IntRange, String> = mapOf(
        0..39 to LorenzColor.GRAY.getChatColor(),
        40..79 to LorenzColor.WHITE.getChatColor(),
        80..119 to LorenzColor.YELLOW.getChatColor(),
        120..159 to LorenzColor.GREEN.getChatColor(),
        160..199 to LorenzColor.DARK_GREEN.getChatColor(),
        200..239 to LorenzColor.AQUA.getChatColor(),
        240..279 to LorenzColor.DARK_AQUA.getChatColor(),
        280..319 to LorenzColor.BLUE.getChatColor(),
        320..359 to LorenzColor.LIGHT_PURPLE.getChatColor(),
        360..399 to LorenzColor.DARK_PURPLE.getChatColor(),
        400..439 to LorenzColor.GOLD.getChatColor(),
        440..479 to LorenzColor.RED.getChatColor(),
        480..Int.MAX_VALUE to LorenzColor.DARK_RED.getChatColor(),
    )

    // The revamped palette: a hex gradient across every tier, enabled via config.
    private val revampedLevelColors: Map<IntRange, String> = mapOf(
        0..39 to hexColor("AAAAAA"), // gray
        40..79 to hexColor("F0F0F0"), // white
        80..119 to hexColor("FFE54C"), // yellow
        120..159 to hexColor("7BE66B"), // green
        160..199 to hexColor("3CA83C"), // dark green
        200..239 to hexColor("5CE6E6"), // aqua
        240..279 to hexColor("2EA6A6"), // dark aqua
        280..319 to hexColor("5C8CFF"), // blue
        320..359 to hexColor("C46BFF"), // light purple
        360..399 to hexColor("8A3CC4"), // dark purple
        400..439 to hexColor("FFB347"), // gold
        440..479 to hexColor("FF5C5C"), // red
        480..519 to hexColor("FF77C2"), // pink
        520..559 to hexColor("E66BFF"), // magenta
        560..599 to hexColor("9D6BFF"), // purple
        600..639 to hexColor("6B8CFF"), // blue
        640..679 to hexColor("5CD6FF"), // light blue
        680..Int.MAX_VALUE to hexColor("5CFFD6"), // mint
    )

    private val levelColors: Map<IntRange, String>
        get() = if (SkyHanniMod.feature.gui.customScoreboard.display.revampedLevelColors) {
            revampedLevelColors
        } else {
            defaultLevelColors
        }

    fun getLevelColor(): String = levelXPPair?.let { getLevelColor(it.first) } ?: LorenzColor.BLACK.getChatColor()

    fun getLevelColor(level: Int): String =
        levelColors.entries.firstOrNull { level in it.key }?.value ?: LorenzColor.BLACK.getChatColor()

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.SB_LEVEL)) return

        TabWidget.SB_LEVEL.matchMatcherFirstLine {
            val level = group("level")?.toIntOrNull()
            val xp = group("xp")?.toIntOrNull()

            updateStorage(level, xp)
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!UtilsPatterns.skyblockMenuGuiPattern.matches(event.inventoryName)) return

        val stack = event.inventoryItems.values.find { itemNamePattern.matches(it.hoverName.formattedTextCompatLeadingWhiteLessResets()) } ?: return

        var level: Int? = null
        var xp: Int? = null

        loop@ for (line in stack.getLore()) {
            if (level != null && xp != null) break@loop

            if (level == null) {
                levelPattern.matchMatcher(line) {
                    level = group("level")?.toIntOrNull()
                    continue@loop
                }
            }

            if (xp == null) {
                xpPattern.matchMatcher(line) {
                    xp = group("xp")?.toIntOrNull()
                    continue@loop
                }
            }
        }

        updateStorage(level, xp)
    }

    private fun updateStorage(level: Int?, xp: Int?) {
        storage = calculateTotalXP(level ?: return, xp ?: return)
    }

    fun calculateTotalXP(level: Int, xp: Int): Int = level * 100 + xp

}
