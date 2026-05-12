package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class TitleScreenConfig {

    @Expose
    @ConfigOption(
        name = "Animated Nebula Background",
        desc = "Replace the panorama on the Minecraft title menu with an animated magenta and burgundy nebula background (main menu only, outside SkyBlock). Works with vanilla titles; leave \"Preserve Pack Core Title Background\" off when Pack Core is installed but its custom menu replacement is disabled.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("title menu, main menu, panorama, splash, backdrop")
    var animatedNebulaBackground: Boolean = true

    @Expose
    @ConfigOption(
        name = "Preserve Pack Core Title Background",
        desc = "When Pack Core is installed, skip SkyHanni's nebula on the Minecraft title menu so Pack Core branding (minimal-style extra buttons over the vanilla panorama) or other behavior can stay visible. Leave this OFF if you use Pack Core's vanilla main-menu option — otherwise the nebula will never draw while Pack Core is loaded.",
    )
    @ConfigEditorBoolean
    @SearchTag("packcore, pack core, title menu")
    var preservePackCoreTitleBackground: Boolean = false

    @Expose
    @ConfigOption(
        name = "Nebula Palette",
        desc = "Five colors blended along the noise gradient (darkest shadows through brightest wisps). Alpha is ignored.",
    )
    @Accordion
    val nebulaPalette: TitleScreenNebulaPaletteConfig = TitleScreenNebulaPaletteConfig()
}

class TitleScreenNebulaPaletteConfig {

    @Expose
    @ConfigOption(name = "Shadow", desc = "Darkest tone (edges of deep areas). Default matches the classic burgundy preset.")
    @ConfigEditorColour
    var shadow: ChromaColour = ChromaColour.fromStaticRGB(14, 5, 8, 255)

    @Expose
    @ConfigOption(name = "Deep", desc = "Second dark tone as brightness rises.")
    @ConfigEditorColour
    var deep: ChromaColour = ChromaColour.fromStaticRGB(31, 6, 15, 255)

    @Expose
    @ConfigOption(name = "Mid", desc = "Mid-tone plasma color.")
    @ConfigEditorColour
    var mid: ChromaColour = ChromaColour.fromStaticRGB(97, 20, 46, 255)

    @Expose
    @ConfigOption(name = "Bright Wisps", desc = "Bright filament and ridge color.")
    @ConfigEditorColour
    var brightWisps: ChromaColour = ChromaColour.fromStaticRGB(255, 82, 133, 255)

    @Expose
    @ConfigOption(name = "Hot Accent", desc = "Strongest highlights on dense wisps.")
    @ConfigEditorColour
    var hotAccent: ChromaColour = ChromaColour.fromStaticRGB(255, 140, 173, 255)
}
