package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import java.awt.Color

class SeaLumiesHighlightConfig {

    @Expose
    @ConfigOption(
        name = "Highlight Sea Lumies",
        desc = "Draws a hitbox outline around Sea Lumies (sea pickles) underwater on Galatea to make farming easier.",
    )
    @FeatureToggle
    @ConfigEditorBoolean
    @SearchTag("moonglade marsh pickle")
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the block hitbox outline.")
    @ConfigEditorColour
    var color: ChromaColour = Color(0, 200, 255).toChromaColor()

    @Expose
    @ConfigOption(
        name = "Outline Opacity",
        desc = "Opacity of the hitbox lines, applied on top of the alpha from the color picker.",
    )
    @ConfigEditorSlider(minValue = 0.15f, maxValue = 1f, minStep = 0.05f)
    var outlineOpacity: Float = 0.75f

    @Expose
    @ConfigOption(
        name = "Hitbox Line Width",
        desc = "Thickness of the outline (higher values are easier to see underwater).",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
    var hitboxLineWidth: Int = 2

    @Expose
    @ConfigOption(
        name = "Min Sea Lumies per Block",
        desc = "Only highlights sea pickle blocks that have at least this many Sea Lumies (1–4). " +
            "Use 1 to highlight every block, or 3 to only show nearly-full stacks.",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 4f, minStep = 1f)
    var minPicklesPerBlock: Int = 1

    @Expose
    @ConfigOption(
        name = "Search Radius",
        desc = "How far around you to scan for Sea Lumies (blocks).",
    )
    @ConfigEditorSlider(minValue = 8f, maxValue = 48f, minStep = 1f)
    var searchRadius: Float = 24f

}
