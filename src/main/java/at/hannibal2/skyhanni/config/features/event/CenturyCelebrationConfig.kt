package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CenturyCelebrationConfig {

    @ConfigOption(
        name = "Daily Highlight",
        desc = "Highlights incomplete daily tasks.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDailyTasks: Boolean = true

    @ConfigOption(
        name = "Raffle Task Tracker",
        desc = "Show a HUD with the remaining Raffle Tasks. Open the §eRaffle Tasks §7menu to update it.\n" +
            "Use the buttons in the overlay to filter by difficulty.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var showRaffleTasks: Boolean = true

    @Expose
    @ConfigLink(owner = CenturyCelebrationConfig::class, field = "showRaffleTasks")
    val raffleTasksPosition: Position = Position(20, 20)

    @ConfigOption(
        name = "Team Finder",
        desc = "Highlight players in the right team when holding a Slice of Cake item.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var teamFinder: Boolean = true

    @ConfigOption(name = "Team Finder Color", desc = "Change all the colors!")
    @Accordion
    @Expose
    val colors: AnniversaryTeamFinderColorConfig = AnniversaryTeamFinderColorConfig()
}
