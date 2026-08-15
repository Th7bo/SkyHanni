package at.hannibal2.skyhanni.config.features.hunting.safari

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * What gets said out loud, and to whom. Every announcement chooses its own audience, since a run is normally four
 * people who each only see their own client's half of it.
 */
class SafariPartyConfig {

    @Expose
    @ConfigOption(name = "Gemzie Stages", desc = "Where the Gemzie encounter stages are posted.")
    @ConfigEditorDropdown
    var gemzie: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(name = "Wumpa Stages", desc = "Where the Wumpa encounter stages are posted.")
    @ConfigEditorDropdown
    var wumpa: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(name = "Doomspiral Stages", desc = "Where the Doomspiral encounter stages are posted.")
    @ConfigEditorDropdown
    var doomspiral: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(name = "Completions", desc = "Where \"<Biome> Done!\" and the whole run milestones are posted.")
    @ConfigEditorDropdown
    var milestones: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(
        name = "Macaw Spawns",
        desc = "Where a Macaw spawn is posted, with its position if the client can see it.",
    )
    @ConfigEditorDropdown
    var macaw: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(
        name = "Sparkling Critters",
        desc = "Where a sparkling critter is posted, with its biome and coordinates.",
    )
    @ConfigEditorDropdown
    var sparkling: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(
        name = "Your Hotspot",
        desc = "Where your Hunting Hotspot is posted.\n" +
            "§7Everyone in the party is told a different one, and only their own client was told.",
    )
    @ConfigEditorDropdown
    var hotspot: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(name = "Hunter Trades", desc = "Where the roaming Hunter NPCs' offers are posted automatically.")
    @ConfigEditorDropdown
    var trades: SafariBroadcast = SafariBroadcast.NONE

    @Expose
    @ConfigOption(name = "Missing List", desc = "Where the missing list goes when you share it by hand.")
    @ConfigEditorDropdown
    var missingList: SafariBroadcast = SafariBroadcast.PARTY

    @Expose
    @ConfigOption(
        name = "Take Partymates' Trades",
        desc = "Read trades announced by other people running SkyHanni, off party chat.\n" +
            "§7A Hunter's dialog is only shown to whoever clicked it, so this is the only way to learn about " +
            "an offer someone else found.",
    )
    @ConfigEditorBoolean
    var acceptSharedTrades: Boolean = true
}
