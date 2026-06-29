package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SkyBlockLevelColorConfig {
    @Expose
    @ConfigOption(
        name = "Rainbow Per Level",
        desc = "Color the SkyBlock Level with a continuous rainbow that shifts hue every single level, " +
            "starting at deep red at level 0.\nOverrides the tier palettes below while enabled.",
    )
    @ConfigEditorBoolean
    var rainbow: Boolean = false

    @Expose
    @ConfigOption(
        name = "Revamped Palette",
        desc = "Use a revamped color palette for the SkyBlock Level, with new hex colors for every tier " +
            "instead of the default vanilla colors.\nApplies everywhere the level is colored.",
    )
    @ConfigEditorBoolean
    var revampedPalette: Boolean = false

    @Expose
    @ConfigOption(
        name = "Color In Tab List",
        desc = "Recolor the SkyBlock level in the Advanced Player Tab List by its tier, " +
            "including the extra hex tiers past level 480.",
    )
    @ConfigEditorBoolean
    var colorInTabList: Boolean = true

    @Expose
    @ConfigOption(
        name = "Color In Nametags",
        desc = "Recolor the SkyBlock level shown above players by its tier, " +
            "including the extra hex tiers past level 480.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var colorInNametags: Boolean = true

    @Expose
    @ConfigOption(
        name = "Color In Chat",
        desc = "Recolor the SkyBlock level in player chat messages by its tier, " +
            "including the extra hex tiers past level 480.\nRequires the §ePlayer Messages§7 chat feature.",
    )
    @ConfigEditorBoolean
    var colorInChat: Boolean = true
}
