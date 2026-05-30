package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForgeDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Display the items currently being forged in the Dwarven Forge and the time remaining on each.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Everywhere",
        desc = "Keep showing the forge display everywhere in SkyBlock.\n" +
            "§eWhen disabled, it only shows in the Dwarven Mines and Crystal Hollows.",
    )
    @ConfigEditorBoolean
    var showEverywhere: Boolean = true

    @Expose
    @ConfigLink(owner = ForgeDisplayConfig::class, field = "enabled")
    val position: Position = Position(10, 10)
}