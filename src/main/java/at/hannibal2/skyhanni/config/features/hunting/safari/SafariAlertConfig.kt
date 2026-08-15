package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

/**
 * The encounter and completion calls.
 *
 * The three staged encounters each announce themselves through ready, started and done, and each is settable on its
 * own, since a party splitting a biome each only ever has one person who can reach any of them.
 */
class SafariAlertConfig {

    @Expose
    @ConfigOption(
        name = "Only In That Biome",
        desc = "Keep each encounter's calls to whoever is standing in its biome.\n" +
            "§7Off, a Wumpa waking up interrupts everyone in the party, including the three who cannot reach it.",
    )
    @ConfigEditorBoolean
    var encountersInBiomeOnly: Boolean = true

    @Expose
    @ConfigOption(name = "Play A Sound", desc = "Play a note alongside every Safari alert.")
    @ConfigEditorBoolean
    var alertSound: Boolean = false

    @Expose
    @ConfigOption(name = "Gemzie Alerts", desc = "Title and chat line as the Gemzie encounter progresses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var gemzie: Boolean = true

    @Expose
    @ConfigOption(name = "Wumpa Alerts", desc = "Title and chat line as the Wumpa encounter progresses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var wumpa: Boolean = true

    @Expose
    @ConfigOption(name = "Doomspiral Alerts", desc = "Title and chat line as the Doomspiral encounter progresses.")
    @ConfigEditorBoolean
    @FeatureToggle
    var doomspiral: Boolean = true

    @Expose
    @ConfigOption(
        name = "Biome Complete",
        desc = "Announce \"<Biome> Done!\" once every species there has been caught by someone.",
    )
    @ConfigEditorBoolean
    var biomeDone: Boolean = false

    @Expose
    @ConfigOption(
        name = "All But Macaw",
        desc = "Announce when the only species left is the Macaw.\n" +
            "§7The Macaw is RNG and not guaranteed at all, so this is usually the real finish line for a run.",
    )
    @ConfigEditorBoolean
    var allButMacaw: Boolean = false

    @Expose
    @ConfigOption(name = "Everything Done", desc = "Announce once all 37 species have been caught by someone.")
    @ConfigEditorBoolean
    var allDone: Boolean = false

    @Expose
    @ConfigOption(
        name = "Macaw Spawned",
        desc = "Title and a chat line the moment a Macaw turns up, from the Birdfeeder or in the world.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var macaw: Boolean = true

    @Expose
    @ConfigOption(
        name = "Every Bird At The Birdfeeder",
        desc = "Call the Bluebird and the Parakeet too, not just the Macaw.\n" +
            "§7They arrive often enough to be wallpaper, which is why they are their own choice.",
    )
    @ConfigEditorBoolean
    var birdfeeder: Boolean = false

    @Expose
    @ConfigOption(name = "Hotspot", desc = "Report your Hunting Hotspot in chat when the run names it.")
    @SearchTag("hunting hotspot biome")
    @ConfigEditorBoolean
    var hotspot: Boolean = true
}
