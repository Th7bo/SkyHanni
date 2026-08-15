package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SafariRunTrackerConfig {

    @Expose
    @ConfigOption(
        name = "Progress Display",
        desc = "Shows a run timer, the party and personal dex progress, and a bar per biome.",
    )
    @SearchTag("critter safari run dex progress")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = SafariRunTrackerConfig::class, field = "enabled")
    val position: Position = Position(10, 80)

    @Expose
    @ConfigOption(
        name = "Show Where",
        desc = "Where the Safari displays appear.\n" +
            "§7The entrance is a separate area over in Torrhus Canyon, so it is its own choice.",
    )
    @ConfigEditorDropdown
    var showWhere: SafariScope = SafariScope.SAFARI_AND_ENTRANCE

    @Expose
    @ConfigOption(
        name = "Count Unique Only",
        desc = "Treat a species as done at the first catch.\n" +
            "§7Off, a species that spawns a fixed number of times stays outstanding until every one of them " +
            "has been caught, since every catch is another shard.",
    )
    @ConfigEditorBoolean
    var uniqueOnly: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Nearby Spawns",
        desc = "Mark a missing species with how many are loaded around you right now.\n" +
            "§7Only what the client can see, which is never the whole map.",
    )
    @ConfigEditorBoolean
    var countSpawns: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hotspot Line",
        desc = "Show which biome is your Hunting Hotspot on the progress display.",
    )
    @ConfigEditorBoolean
    var showHotspot: Boolean = true

    @Expose
    @ConfigOption(name = "Per Player Lines", desc = "Show who is covering which biome, under the biome bars.")
    @ConfigEditorBoolean
    var showPerPlayer: Boolean = true

    enum class SafariScope(private val displayName: String) {
        /** Inside the Safari only - nothing at the entrance over in Torrhus Canyon. */
        SAFARI("Only in the Safari"),

        /** Inside and at the entrance, which is where a run is about to start. */
        SAFARI_AND_ENTRANCE("Safari and entrance"),

        /** Wherever you are, Safari or not. */
        EVERYWHERE("Everywhere"),
        ;

        override fun toString() = displayName
    }
}
