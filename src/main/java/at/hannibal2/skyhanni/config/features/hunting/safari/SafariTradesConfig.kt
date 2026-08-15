package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

/**
 * The roaming Hunter NPCs offer one shard for one quest item each. Their dialog is only shown to whoever clicked
 * them, so an offer routinely goes unused while a partymate is carrying exactly the item it wants.
 */
class SafariTradesConfig {

    @Expose
    @ConfigOption(
        name = "Report Trades",
        desc = "Report every Hunter's shard for item offer in chat, with the biome and coordinates it was found at.",
    )
    @SearchTag("huntress hunter shard gem trade")
    @ConfigEditorBoolean
    @FeatureToggle
    var report: Boolean = true

    @Expose
    @ConfigOption(
        name = "Trade Display",
        desc = "A display listing the offers found this run, nearest first.\n" +
            "§7The chat report scrolls away; this keeps them to hand for a quest item picked up later.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var tracker: Boolean = true

    @Expose
    @ConfigLink(owner = SafariTradesConfig::class, field = "tracker")
    val position: Position = Position(-200, 250)
}
