package at.hannibal2.skyhanni.config.features.inventory.sacks

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class OutsideSackValueConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the value of all items in the sacks as GUI, while not being in the sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Change Price Source",
        desc = "Change what price to use: the Bazaar price range (Instant Sell - Instant Buy) or the NPC sell price."
    )
    @ConfigEditorDropdown
    val priceSource: Property<PriceSourceEntry> = Property.of(PriceSourceEntry.BAZAAR)

    enum class PriceSourceEntry(private val displayName: String) {
        BAZAAR("Bazaar Range"),
        NPC_SELL("NPC Sell"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = OutsideSackValueConfig::class, field = "enabled")
    val position: Position = Position(144, 139)
}
