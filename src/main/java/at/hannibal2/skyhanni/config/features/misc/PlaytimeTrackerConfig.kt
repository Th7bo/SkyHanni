package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.misc.playtimetracker.PlaytimeTrackerGui
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class PlaytimeTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tracks time spent in SkyBlock per calendar day (local timezone). Saves a history per day.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Average Window",
        desc = "Rolling window in days used for the §eAverage/day§r display (today plus the prior days).\n§eExample:§r 7 = average over up to the last week.\n§7If fewer days have records in that range, the average uses only those recorded days.",
    )
    @ConfigEditorSlider(minValue = 2f, maxValue = 90f, minStep = 1f)
    var averageWindowDays: Float = 7f

    @Expose
    @ConfigOption(
        name = "History Length",
        desc = "How many calendar days of history to keep (older days are dropped).",
    )
    @ConfigEditorSlider(minValue = 14f, maxValue = 730f, minStep = 1f)
    var historyRetentionDays: Float = 366f

    @Expose
    @ConfigOption(
        name = "Track All-Time Max",
        desc = "Show the highest single-day SkyBlock playtime ever recorded in the HUD overlay and tracker GUI.\n§7Works with previously recorded data.",
    )
    @ConfigEditorBoolean
    var showMaxPlaytime: Boolean = false

    @Expose
    @ConfigOption(
        name = "HUD Overlay",
        desc = "Show today's playtime and rolling average while in SkyBlock.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showOverlay: Boolean = false

    @Expose
    @ConfigLink(owner = PlaytimeTrackerConfig::class, field = "showOverlay")
    val overlayPosition: Position = Position(20, 120)

    @Expose
    @ConfigOption(name = "Open Tracker GUI", desc = "Shows daily totals, averages, and full history.")
    @ConfigEditorButton(buttonText = "Open")
    val openTrackerGui: Runnable = Runnable {
        MinecraftCompat.screen = PlaytimeTrackerGui()
    }

    @Expose
    @ConfigOption(name = "Open GUI Hotkey", desc = "Key to open the tracker screen (outside inventories).")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var openGuiHotkey: Int = GLFW.GLFW_KEY_UNKNOWN
}
