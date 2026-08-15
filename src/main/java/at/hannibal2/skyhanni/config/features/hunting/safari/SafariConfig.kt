package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SafariConfig {

    @Expose
    @ConfigOption(
        name = "Names in Center",
        desc = "Shows the names of the 4 areas while in the center of the Critter Safari.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var namesInCenter: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hideyho Finder",
        desc = "Walks you around the known hiding spots after Hideyho hides.\n" +
            "§7For the mark on where it actually is, see §eHighlights → Hideyho Solver§7.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideyhoFinder: Boolean = true

    @Expose
    @ConfigOption(
        name = "Share Hunter Trade",
        desc = "Shows a clickable message to share the trade a Hunter offers with your party, including their location.",
    )
    @SearchTag("huntress shard gem")
    @ConfigEditorBoolean
    @FeatureToggle
    var tradeShare: Boolean = true

    @Expose
    @ConfigOption(
        name = "Remove The Darkness Effect",
        desc = "Drop the Warden darkness while you are at the Safari.\n" +
            "§7Nothing is sent to the server; the client simply does not draw something it was told about.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var removeDarkness: Boolean = false

    @Expose
    @ConfigOption(name = "Sparkling Notifier", desc = "")
    @Accordion
    val sparklingNotifier = SparklingNotifierConfig()

    @Expose
    @ConfigOption(name = "Run Tracker", desc = "Track what this party has caught since walking in.")
    @Accordion
    val runTracker = SafariRunTrackerConfig()

    @Expose
    @ConfigOption(name = "Missing List", desc = "What is still uncaught in the biome you are standing in.")
    @Accordion
    val missing = SafariMissingConfig()

    @Expose
    @ConfigOption(name = "Hunter Trades", desc = "The roaming Hunter NPCs' shard for item offers.")
    @Accordion
    val trades = SafariTradesConfig()

    @Expose
    @ConfigOption(name = "Highlights", desc = "The marks drawn in the world.")
    @Accordion
    val highlights = SafariHighlightConfig()

    @Expose
    @ConfigOption(name = "Alerts", desc = "Encounter and completion calls.")
    @Accordion
    val alerts = SafariAlertConfig()

    @Expose
    @ConfigOption(name = "Party Announcements", desc = "What gets announced to your team, and where.")
    @Accordion
    val party = SafariPartyConfig()

    @Expose
    @ConfigOption(name = "Shard Value", desc = "What the shards a run gives are worth.")
    @Accordion
    val profit = SafariProfitConfig()
}
