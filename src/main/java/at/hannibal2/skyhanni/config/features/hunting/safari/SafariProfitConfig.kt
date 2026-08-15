package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

/**
 * What a run's shards are worth.
 *
 * A catch is not a shard and a shard is not a coin: every catch gives a random number of shards, and each species'
 * shard trades for a wildly different amount, so nothing about money can be derived from the catch counts.
 */
class SafariProfitConfig {

    @Expose
    @ConfigOption(
        name = "Track Shard Value",
        desc = "Price the shards a run gives, at bazaar prices.",
    )
    @SearchTag("critter safari shard value coins profit")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Price",
        desc = "Which side of the spread a shard is counted at.\n" +
            "§7§lInstant sell§r§7 is what selling to the bazaar pays you this second. §lSell offer§r§7 is what " +
            "you would list at and wait for. The second is the bigger number and the slower money.",
    )
    @ConfigEditorDropdown
    var priceSource: SafariPriceSource = SafariPriceSource.INSTANT_SELL

    @Expose
    @ConfigOption(
        name = "Show Both Prices",
        desc = "Put the other side of the spread next to the run's value.",
    )
    @ConfigEditorBoolean
    var showBothPrices: Boolean = false

    @Expose
    @ConfigOption(
        name = "Subtract Bazaar Tax",
        desc = "Take the 1.25% bazaar tax off, since both prices are a sell.",
    )
    @ConfigEditorBoolean
    var subtractTax: Boolean = true

    @Expose
    @ConfigOption(
        name = "Value On The Progress Display",
        desc = "Show what this run has earned, under the biome bars.",
    )
    @ConfigEditorBoolean
    var showOnDisplay: Boolean = true

    enum class SafariPriceSource(private val displayName: String) {
        /** The best standing buy order - what selling to the bazaar pays right now. */
        INSTANT_SELL("Instant sell"),

        /** The cheapest standing sell offer - what you would list at, and wait for. */
        SELL_OFFER("Sell offer"),
        ;

        override fun toString() = displayName
    }
}
