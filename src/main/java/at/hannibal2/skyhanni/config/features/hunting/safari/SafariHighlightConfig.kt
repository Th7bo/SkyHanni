package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import java.awt.Color

/**
 * The in world marks. Everything here is off by default apart from the hard to find critters, and everything that
 * belongs to one biome is only drawn while you are standing in that biome.
 */
class SafariHighlightConfig {

    @Expose
    @ConfigOption(
        name = "Snooper Walls",
        desc = "Mark unbroken Snooper walls, while you are in the Cavern.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var snooperWalls: Boolean = false

    @Expose
    @ConfigOption(
        name = "Troodon Walls",
        desc = "Mark unbroken Troodon walls, while you are in the Icy biome.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var troodonWalls: Boolean = false

    @Expose
    @ConfigOption(
        name = "Bee Nests",
        desc = "Mark bee nests you have not punched yet, while you are in the Forest.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var nests: Boolean = false

    @Expose
    @ConfigOption(name = "Hunter Trades", desc = "Mark where a Hunter offered a trade.")
    @ConfigEditorBoolean
    @FeatureToggle
    var trades: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hideonwall Perches",
        desc = "Mark the eight places a Hideonwall turns up, while you are in the Haunted biome.\n" +
            "§7Nothing about a perch looks different when it is empty, so the mark means \"check here\".",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideonwallPerches: Boolean = false

    @Expose
    @ConfigOption(
        name = "Rockmite Mounds",
        desc = "Outline detected Rockmite mounds, while you are in the Cavern.\n" +
            "§7Mounds are interaction entities rather than blocks, so they are found by their hitbox.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var mounds: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hideyho Solver",
        desc = "Mark where Hideyho is actually hiding, while you are in the Haunted biome.\n" +
            "§7Hiding only moves it: its nametag stays loaded on the client the whole time, so this is where it " +
            "is rather than a list of known hiding spots.\n" +
            "§7The mark survives the entity unloading, so one pass within range is enough.",
    )
    @SearchTag("hide and seek hideyho")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideyhoSolver: Boolean = false

    @Expose
    @ConfigOption(
        name = "Recatch Helper",
        desc = "Pin where a critter was when you threw a capsule at it.\n" +
            "§7A failed capture puts it back on the spot it left from, so the next capsule can already be aimed.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var recatchHelper: Boolean = false

    @Expose
    @ConfigOption(
        name = "Floor Drops",
        desc = "Mark the drops lying on the floor of the biome you are in.\n" +
            "§7§lHighlight§r§7 draws a box where it is, seen only when you could see the drop anyway. " +
            "§lWaypoint§r§7 draws it through the terrain, named and with its distance.",
    )
    @ConfigEditorDropdown
    var floorDrops: MarkStyle = MarkStyle.OFF

    @Expose
    @ConfigOption(
        name = "Hard To Find Critters",
        desc = "Box the mobs of species that are awkward to spot, Bloodbats especially.\n" +
            "§7Depth tested, so it marks one you could see anyway.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hardToFind: Boolean = true

    @Expose
    @ConfigOption(name = "Colors", desc = "The color of each kind of mark.")
    @Accordion
    val colors: ColorConfig = ColorConfig()

    class ColorConfig {
        @Expose
        @ConfigOption(name = "Snooper Walls", desc = "")
        @ConfigEditorColour
        var snooperWalls: ChromaColour = Color(0xFF, 0xAA, 0x00).toChromaColor()

        @Expose
        @ConfigOption(name = "Troodon Walls", desc = "")
        @ConfigEditorColour
        var troodonWalls: ChromaColour = Color(0x55, 0xAA, 0xFF).toChromaColor()

        @Expose
        @ConfigOption(name = "Bee Nests", desc = "")
        @ConfigEditorColour
        var nests: ChromaColour = Color(0x55, 0xFF, 0x55).toChromaColor()

        @Expose
        @ConfigOption(name = "Hunter Trades", desc = "")
        @ConfigEditorColour
        var trades: ChromaColour = Color(0x55, 0xFF, 0xFF).toChromaColor()

        @Expose
        @ConfigOption(name = "Hideonwall Perches", desc = "")
        @ConfigEditorColour
        var hideonwallPerches: ChromaColour = Color(0xAA, 0x55, 0xFF).toChromaColor()

        @Expose
        @ConfigOption(name = "Rockmite Mounds", desc = "")
        @ConfigEditorColour
        var mounds: ChromaColour = Color(0xCC, 0x77, 0x44).toChromaColor()

        @Expose
        @ConfigOption(name = "Hideyho", desc = "")
        @ConfigEditorColour
        var hideyho: ChromaColour = Color(0xFF, 0x55, 0xFF).toChromaColor()

        @Expose
        @ConfigOption(name = "Recatch Spot", desc = "")
        @ConfigEditorColour
        var recatch: ChromaColour = Color(0xFF, 0xFF, 0x55).toChromaColor()

        @Expose
        @ConfigOption(name = "Floor Drops", desc = "")
        @ConfigEditorColour
        var floorDrops: ChromaColour = Color(0x55, 0xFF, 0xAA).toChromaColor()

        @Expose
        @ConfigOption(name = "Hard To Find Critters", desc = "")
        @ConfigEditorColour
        var hardToFind: ChromaColour = Color(0xFF, 0x55, 0x55).toChromaColor()
    }

    enum class MarkStyle(private val displayName: String) {
        /** Not drawn at all. */
        OFF("Nothing"),

        /** A box where it is, visible only when you could see the thing anyway. */
        HIGHLIGHT("Highlight"),

        /** A box through the terrain, named, with its distance. */
        WAYPOINT("Waypoint"),
        ;

        override fun toString() = displayName
    }
}
