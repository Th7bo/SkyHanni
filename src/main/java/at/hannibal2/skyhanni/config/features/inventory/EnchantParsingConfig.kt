package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.misc.items.enchants.EnchantParser
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class EnchantParsingConfig {
    @Expose
    @ConfigOption(
        name = "Enable",
        desc = "Toggle for coloring the enchants. Turn this off if you want to use enchant parsing from other mods."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val colorParsing: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Format", desc = "The way the enchants are formatted in the tooltip.")
    @ConfigEditorDropdown
    val format: Property<EnchantFormat> = Property.of(EnchantFormat.NORMAL)

    enum class EnchantFormat(private val displayName: String) {
        NORMAL("Normal"),
        COMPRESSED("Compressed"),
        STACKED("Stacked"),
        ;

        override fun toString() = displayName
    }

    @ConfigOption(
        name = "§cChroma Warning",
        desc = "Chroma requires a separate setting.\n§eIf SkyHanni chroma is disabled Chroma will default to §6Gold.",
    )
    @ConfigEditorButton(buttonText = "Go")
    val chromaRunnable = Runnable { EnchantParser.openConfigLink() }

    @Expose
    @ConfigOption(
        name = "Ultimate Enchantment Color",
        desc = "The color the Ultimate enchantment will be. (Will always be bold)\n" +
            "§eEnable chroma in the color picker to use SkyHanni's chroma.",
    )
    @ConfigEditorColour
    val ultimateEnchantColor: Property<ChromaColour> = Property.of(defaultUltimateColor())

    @Expose
    @ConfigOption(
        name = "Perfect Enchantment Color",
        desc = "The color an enchantment will be at max level.\n" +
            "§eEnable chroma in the color picker to use SkyHanni's chroma.",
    )
    @ConfigEditorColour
    val perfectEnchantColor: Property<ChromaColour> = Property.of(defaultChromaColor())

    @Expose
    @ConfigOption(name = "Perfect Enchantment Bold", desc = "Enchantments at max level will be bold.")
    @ConfigEditorBoolean
    val boldPerfectEnchant: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Great Enchantment Color", desc = "The color an enchantment will be at a great level.")
    @ConfigEditorColour
    val greatEnchantColor: Property<ChromaColour> = Property.of(LorenzColor.GOLD.toChromaColor())

    @Expose
    @ConfigOption(name = "Good Enchantment Color", desc = "The color an enchantment will be at a good level.")
    @ConfigEditorColour
    val goodEnchantColor: Property<ChromaColour> = Property.of(LorenzColor.BLUE.toChromaColor())

    @Expose
    @ConfigOption(name = "Poor Enchantment Color", desc = "The color an enchantment will be at a poor level.")
    @ConfigEditorColour
    val poorEnchantColor: Property<ChromaColour> = Property.of(LorenzColor.GRAY.toChromaColor())

    @Expose
    @ConfigOption(
        name = "Hide Vanilla Enchants",
        desc = "Hide the regular vanilla enchants usually found in the first 1-2 lines of lore."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val hideVanillaEnchants: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Hide Enchant Description",
        desc = "Hide the enchant description after each enchant if available."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val hideEnchantDescriptions: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Stacking Enchant Progress",
        desc = "Shows the stacking enchant progress at the bottom of the lore. " +
            "§eRequires Enchant Parsing to be enabled."
    )
    @ConfigEditorBoolean
    var stackingEnchantProgress: Boolean = true

    companion object {
        // A white base color with a non-zero rotation time marks a color as chroma. The base color
        // is irrelevant since chroma enchants are rendered through SkyHanni's chroma shader instead.
        private const val CHROMA_ROTATION_MILLIS = 2000

        fun defaultChromaColor(): ChromaColour = ChromaColour.fromRGB(255, 255, 255, CHROMA_ROTATION_MILLIS, 255)

        fun defaultUltimateColor(): ChromaColour = LorenzColor.LIGHT_PURPLE.toChromaColor()
    }
}
