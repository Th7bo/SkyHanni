package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.data.IslandType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RareDropAnimationConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Play a totem-of-undying style animation when you receive a rare drop.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Animation Style",
        desc = "§eCustom§7: hand-crafted center-screen animation with configurable duration, scale, and flash.\n" +
            "§eVanilla§7: the real totem-of-undying animation with particles.",
    )
    @ConfigEditorDropdown
    var animationStyle: AnimationStyle = AnimationStyle.CUSTOM

    enum class AnimationStyle(private val displayName: String) {
        CUSTOM("Custom"),
        VANILLA("Vanilla (Totem)"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Animation Duration",
        desc = "How long the animation plays, in seconds. §8(Custom only)",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.5f)
    var duration: Float = 3f

    @Expose
    @ConfigOption(
        name = "Item Scale",
        desc = "How large the dropped item appears in the center of the screen. §8(Custom only)",
    )
    @ConfigEditorSlider(minValue = 2f, maxValue = 10f, minStep = 1f)
    var itemScale: Float = 6f

    @Expose
    @ConfigOption(
        name = "Show Item Name",
        desc = "Show the item name as text during the animation. §8(Custom only)",
    )
    @ConfigEditorBoolean
    var showItemName: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Background Flash",
        desc = "Flash a rarity-colored background behind the item. §8(Custom only)",
    )
    @ConfigEditorBoolean
    var showFlash: Boolean = true

    @Expose
    @ConfigOption(
        name = "Play Sound",
        desc = "Play a sound when the rare drop animation triggers.",
    )
    @ConfigEditorBoolean
    var playSound: Boolean = true

    @Expose
    @ConfigOption(
        name = "Include Pet Drops",
        desc = "Also trigger the animation for §6PET DROP! §7messages.",
    )
    @ConfigEditorBoolean
    var includePetDrops: Boolean = true

    @Expose
    @ConfigOption(
        name = "Include Trophy Fish",
        desc = "Also trigger the animation for Outstanding/Great fishing catches.",
    )
    @ConfigEditorBoolean
    var includeTrophyFish: Boolean = true

    // ── Ignore system ────────────────────────────────────────────────────────

    @ConfigOption(
        name = "§eIgnore — by Location",
        desc = "Suppress all animations while on any of the listed islands.\n" +
            "§eDrag an island out of the list to stop ignoring it.",
    )
    @ConfigEditorInfoText
    var ignoredLocationsInfo: String = ""

    @Expose
    @ConfigOption(
        name = "Ignored Locations",
        desc = "§7Islands listed here will §cnever §7trigger the animation.",
    )
    @ConfigEditorDraggableList
    var ignoredLocations: MutableList<IgnoredLocationEntry> = mutableListOf()

    enum class IgnoredLocationEntry(val islands: Set<IslandType>, private val displayName: String) {
        DUNGEONS(setOf(IslandType.CATACOMBS), "§aDungeons"),
        SPIDER_DEN(setOf(IslandType.SPIDER_DEN), "§9Spider's Den"),
        THE_END(setOf(IslandType.THE_END), "§5The End"),
        CRIMSON_ISLE(setOf(IslandType.CRIMSON_ISLE), "§6Crimson Isle"),
        KUUDRA(setOf(IslandType.KUUDRA_ARENA), "§cKuudra"),
        CRYSTAL_HOLLOWS(setOf(IslandType.CRYSTAL_HOLLOWS), "§bCrystal Hollows"),
        DWARVEN_MINES(setOf(IslandType.DWARVEN_MINES, IslandType.MINESHAFT), "§7Dwarven Mines"),
        DEEP_CAVERNS(setOf(IslandType.DEEP_CAVERNS, IslandType.GOLD_MINES), "§8Deep Caverns"),
        HUB(setOf(IslandType.HUB), "§fHub"),
        GARDEN(setOf(IslandType.GARDEN, IslandType.GARDEN_GUEST), "§2Garden"),
        THE_RIFT(setOf(IslandType.THE_RIFT), "§dThe Rift"),
        PRIVATE_ISLAND(setOf(IslandType.PRIVATE_ISLAND, IslandType.PRIVATE_ISLAND_GUEST), "§ePrivate Island"),
        ;

        override fun toString() = displayName
    }

    @ConfigOption(
        name = "§eIgnore — Specific Items",
        desc = "When an animation plays, a §c[Ignore]§7 hint appears in chat.\n" +
            "Click it to add that specific item to this list.\n" +
            "§8Internal names are stored — renames won't affect matching.",
    )
    @ConfigEditorInfoText
    var ignoredItemsInfo: String = ""

    @Expose
    @ConfigOption(
        name = "Show Ignore Hint",
        desc = "Print a clickable §c[Ignore]§7 line in chat when the animation plays,\n" +
            "so you can quickly suppress that item in the future.",
    )
    @ConfigEditorBoolean
    var showIgnoreHint: Boolean = true

    @Expose
    @ConfigOption(
        name = "Ignored Item Type",
        desc = "Choose whether clicking §c[Ignore]§7 ignores that exact item\n" +
            "or all items of the same §7category§7 (e.g. all Enchanted Books).",
    )
    @ConfigEditorDropdown
    var ignoreMode: IgnoreModeEntry = IgnoreModeEntry.SPECIFIC_ITEM

    enum class IgnoreModeEntry(private val displayName: String) {
        SPECIFIC_ITEM("Specific item only"),
        CATEGORY("Entire item category"),
        ;

        override fun toString() = displayName
    }

    /** Internal names of items the user has clicked [Ignore] for. */
    @Expose
    var customIgnoredItems: MutableList<String> = mutableListOf()

    @ConfigOption(
        name = "Clear Ignored Items",
        desc = "Remove all specific items you have ignored via the chat §c[Ignore]§7 button.",
    )
    @ConfigEditorButton(buttonText = "Clear")
    val clearCustomIgnoredItems: Runnable = Runnable {
        customIgnoredItems.clear()
    }
}
