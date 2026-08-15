package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SafariMissingConfig {

    @Expose
    @ConfigOption(
        name = "Missing Display",
        desc = "Lists what is still uncaught in the biome you are standing in.\n" +
            "§7Species already caught by a partymate count as done, since the run's goal is party wide coverage.",
    )
    @SearchTag("critter safari missing uncaught")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = SafariMissingConfig::class, field = "enabled")
    val position: Position = Position(-200, 80)

    @Expose
    @ConfigOption(
        name = "Snooper Walls",
        desc = "List the breakable Cavern walls still standing, under the missing list.",
    )
    @ConfigEditorBoolean
    var showSnooperWalls: Boolean = true

    @Expose
    @ConfigOption(
        name = "Troodon Walls",
        desc = "List the breakable Icy walls still standing, under the missing list.",
    )
    @ConfigEditorBoolean
    var showTroodonWalls: Boolean = true

    @Expose
    @ConfigOption(
        name = "Bee Nests",
        desc = "List the bee nests still to punch, under the Forest missing list.\n" +
            "§7Counts nests you have come across, not nests on the map.",
    )
    @ConfigEditorBoolean
    var showNests: Boolean = true

    @Expose
    @ConfigOption(
        name = "Rockmite Mounds",
        desc = "Count the mounds still standing near you, under the Cavern missing list.",
    )
    @ConfigEditorBoolean
    var showMoundCount: Boolean = true

    @Expose
    @ConfigOption(
        name = "Mounds Broken",
        desc = "Count the mounds broken this run and how many held a Rockmite.\n" +
            "§7Only about a fifth of them do, so the number of rolls taken is worth having on its own.",
    )
    @ConfigEditorBoolean
    var showMoundsBroken: Boolean = false
}
