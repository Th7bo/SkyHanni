package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.misc.LevelColorGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import java.awt.Color

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

    // The default palette: the vanilla named colors for 0-479, then new hex tiers past the old level-480 cap.
    private val defaultLevelColors: Map<IntRange, Int> = mapOf(
        0..39 to LorenzColor.GRAY.toRgb(),
        40..79 to LorenzColor.WHITE.toRgb(),
        80..119 to LorenzColor.YELLOW.toRgb(),
        120..159 to LorenzColor.GREEN.toRgb(),
        160..199 to LorenzColor.DARK_GREEN.toRgb(),
        200..239 to LorenzColor.AQUA.toRgb(),
        240..279 to LorenzColor.DARK_AQUA.toRgb(),
        280..319 to LorenzColor.BLUE.toRgb(),
        320..359 to LorenzColor.LIGHT_PURPLE.toRgb(),
        360..399 to LorenzColor.DARK_PURPLE.toRgb(),
        400..439 to LorenzColor.GOLD.toRgb(),
        440..479 to LorenzColor.RED.toRgb(),
        480..519 to LorenzColor.DARK_RED.toRgb(), // the original last vanilla tier, kept
        // The named-color palette is exhausted past here. These tiers use a distinct rose -> violet
        // "endgame" ramp that deliberately avoids the vanilla hues above.
        520..559 to 0xFF6FA5, // rose
        560..599 to 0xFF3DB0, // deep pink
        600..639 to 0xC24DFF, // vivid purple
        640..679 to 0x9B5BFF, // amethyst
        680..719 to 0x6E72FF, // periwinkle
        720..759 to 0x5566F0, // indigo
        760..Int.MAX_VALUE to 0xC9BCFF, // pale lavender
    )

    // The revamped palette: a hex gradient across every tier, enabled via config.
    private val revampedLevelColors: Map<IntRange, Int> = mapOf(
        0..39 to 0xAAAAAA, // gray
        40..79 to 0xF0F0F0, // white
        80..119 to 0xFFE54C, // yellow
        120..159 to 0x7BE66B, // green
        160..199 to 0x3CA83C, // dark green
        200..239 to 0x5CE6E6, // aqua
        240..279 to 0x2EA6A6, // dark aqua
        280..319 to 0x5C8CFF, // blue
        320..359 to 0xC46BFF, // light purple
        360..399 to 0x8A3CC4, // dark purple
        400..439 to 0xFFB347, // gold
        440..479 to 0xFF5C5C, // red
        480..519 to 0xB33C3C, // dark red, kept as the last "base" tier
        // Distinct rose -> violet "endgame" ramp past the old level-480 cap.
        520..559 to 0xFF6FA5, // rose
        560..599 to 0xFF3DB0, // deep pink
        600..639 to 0xC24DFF, // vivid purple
        640..679 to 0x9B5BFF, // amethyst
        680..719 to 0x6E72FF, // periwinkle
        720..Int.MAX_VALUE to 0x5566F0, // indigo
    )

    private fun LorenzColor.toRgb(): Int = toColor().rgb and 0xFFFFFF

    private val levelColors: Map<IntRange, Int>
        get() = if (config.revampedPalette) revampedLevelColors else defaultLevelColors

    // The level at which the rainbow reaches its final hue. Spread wider than the level cap so realistic
    // levels never repeat a color.
    private const val RAINBOW_LEVEL_SPAN = 600f

    // Stop short of a full loop so the sweep never wraps back to the red it started on.
    private const val RAINBOW_MAX_HUE = 0.85f

    /** A per-level rainbow hue, starting at deep red at level 0 and shifting up every level without repeating. */
    private fun rainbowColorRgb(level: Int): Int {
        val hue = (level / RAINBOW_LEVEL_SPAN).coerceIn(0f, 1f) * RAINBOW_MAX_HUE
        return Color.HSBtoRGB(hue, 1f, 0.8f) and 0xFFFFFF
    }

    private val config get() = SkyHanniMod.feature.gui.skyBlockLevelColors

    /** The tiers of the currently active palette, ordered from lowest level to highest. */
    fun getLevelColorTiers(): List<Pair<IntRange, Int>> = levelColors.entries.map { it.key to it.value }

    val usingRevampedPalette: Boolean get() = config.revampedPalette
    val usingRainbow: Boolean get() = config.rainbow
    val colorInChat: Boolean get() = config.colorInChat

    /** The exact tier color as RGB, or the per-level rainbow color when that mode is enabled. */
    fun getLevelColorRgb(level: Int): Int =
        if (config.rainbow) rainbowColorRgb(level)
        else levelColors.entries.firstOrNull { level in it.key }?.value ?: 0

    /**
     * The level wrapped in a component styled with the exact tier color (true hex).
     * Use this for surfaces rendered through Minecraft components (tab list, nametags).
     */
    fun getLevelColorComponent(level: Int, text: String): MutableComponent =
        text.asComponent().withColor(TextColor.fromRgb(getLevelColorRgb(level)))

    /**
     * The nearest legacy chat color code for the tier.
     * The hex tiers are approximated since legacy strings cannot represent hex; use this only for
     * string-only surfaces such as the Custom Scoreboard.
     */
    fun getLevelChatColor(level: Int): String = nearestLegacyColor(getLevelColorRgb(level)).getChatColor()

    fun getLevelChatColor(): String =
        levelXPPair?.let { getLevelChatColor(it.first) } ?: LorenzColor.BLACK.getChatColor()

    private fun nearestLegacyColor(rgb: Int): LorenzColor {
        val target = Color(rgb)
        return LorenzColor.entries.filter { it != LorenzColor.CHROMA }.minByOrNull {
            val c = it.toColor()
            val dr = c.red - target.red
            val dg = c.green - target.green
            val db = c.blue - target.blue
            dr * dr + dg * dg + db * db
        } ?: LorenzColor.WHITE
    }

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

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shlevelcolors") {
            description = "Opens a GUI showing every SkyBlock Level color tier of the active palette."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                // Deferred so the closing chat screen doesn't immediately override our screen.
                DelayedRun.runNextTick {
                    Minecraft.getInstance().setScreen(LevelColorGui())
                }
            }
        }
    }

}
