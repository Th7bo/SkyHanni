package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import java.awt.Color

class SparklingNotifierConfig {

    @Expose
    @ConfigOption(
        name = "Sparkling Notifier",
        desc = "Notifies you when a Sparkling mob spawns in the Critter Safari, and highlights it through walls.",
    )
    @SearchTag("sparkle shiny")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color for the Sparkling mob highlight.")
    @ConfigEditorColour
    var color: ChromaColour = Color.MAGENTA.toChromaColor()

}
