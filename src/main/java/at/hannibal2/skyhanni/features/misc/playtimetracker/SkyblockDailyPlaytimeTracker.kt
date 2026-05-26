package at.hannibal2.skyhanni.features.misc.playtimetracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage.DailySkyblockPlaytimeStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkyblockDailyPlaytimeTracker {

    private val config get() = SkyHanniMod.feature.misc.dailyPlaytime

    private fun storage(): DailySkyblockPlaytimeStorage? = ProfileStorageData.playerSpecific?.skyblockDailyPlaytime

    /**
     * Average seconds per calendar day over the rolling window ending today.
     * Days with no stored row in that window are ignored (both sum and divisor), so if fewer than
     * [windowDays] days were ever recorded, the divisor is that smaller count.
     */
    fun computeRollingAverageDailySeconds(windowDays: Int): Double {
        val end = LocalDate.now()
        val span = windowDays.coerceIn(2, 90)
        val stor = storage() ?: return 0.0
        val map = stor.secondsByIsoDate
        var sum = 0L
        var recordedDays = 0
        for (i in 0 until span) {
            val iso = end.minusDays(i.toLong()).toString()
            val secs = map[iso] ?: continue
            sum += secs
            recordedDays++
        }
        if (recordedDays == 0) return 0.0
        return sum.toDouble() / recordedDays
    }

    fun secondsForCalendarDay(day: LocalDate): Long =
        storage()?.secondsByIsoDate?.get(day.toString()) ?: 0L

    /** Returns the all-time highest single-day playtime in seconds together with the date it was achieved. */
    fun computeAllTimeMax(): Pair<Long, LocalDate?> {
        val stor = storage() ?: return 0L to null
        val date = stor.allTimeMaxDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return stor.allTimeMaxSeconds to date
    }

    /** Short human-readable duration (hours/minutes preference). */
    fun formatSeconds(totalSeconds: Long): String =
        totalSeconds.seconds.format(biggestUnit = TimeUnit.HOUR, maxUnits = 2)

    private fun prune(stor: DailySkyblockPlaytimeStorage) {
        val retention = config.historyRetentionDays.toInt().coerceIn(14, 730)
        val today = LocalDate.now()
        val oldestKeep = today.minusDays(retention.toLong() - 1)
        stor.secondsByIsoDate.keys.removeAll { key ->
            val d = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@removeAll false
            d.isBefore(oldestKeep)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled) return
        val stor = storage() ?: return
        val todayIso = LocalDate.now().toString()
        stor.secondsByIsoDate.addOrPut(todayIso, 1L)

        // One-time migration: initialize stored max from history for users upgrading from older versions.
        if (stor.allTimeMaxSeconds == 0L && stor.secondsByIsoDate.isNotEmpty()) {
            val best = stor.secondsByIsoDate.maxByOrNull { it.value }
            if (best != null) {
                stor.allTimeMaxSeconds = best.value
                stor.allTimeMaxDate = best.key
            }
        }

        val todayTotal = stor.secondsByIsoDate[todayIso] ?: 0L
        if (todayTotal > stor.allTimeMaxSeconds) {
            stor.allTimeMaxSeconds = todayTotal
            stor.allTimeMaxDate = todayIso
        }

        if (event.repeatSeconds(60)) {
            prune(stor)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiOverlay(@Suppress("UNUSED_PARAMETER") event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled || !config.showOverlay) return
        val today = LocalDate.now().toString()
        val secs = storage()?.secondsByIsoDate?.get(today) ?: 0L
        val avg = computeRollingAverageDailySeconds(config.averageWindowDays.toInt().coerceIn(2, 90)).toLong()
        val window = config.averageWindowDays.toInt().coerceIn(2, 90)
        val overlayLines = buildList {
            add(Renderable.text("§eSB play today: §f${formatSeconds(secs)}"))
            add(Renderable.text("§eAvg §7(${window}d): §f${formatSeconds(avg)}"))
            if (config.showMaxPlaytime) {
                val (maxSecs, maxDate) = computeAllTimeMax()
                val dateSuffix = if (maxDate != null) " §7($maxDate)" else ""
                add(Renderable.text("§eAll-time max: §f${formatSeconds(maxSecs)}$dateSuffix"))
            }
        }
        val lines = Renderable.vertical(overlayLines, spacing = 2)
        config.overlayPosition.renderRenderable(lines, posLabel = "Daily SkyBlock playtime")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdailyplaytime") {
            description = "Opens the SkyBlock daily playtime tracker GUI with per-day history and averages."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                Minecraft.getInstance().setScreen(PlaytimeTrackerGui())
            }
        }
    }

    @HandleEvent
    fun onKeyDown(event: KeyDownEvent) {
        val key = config.openGuiHotkey
        if (key == GLFW.GLFW_KEY_UNKNOWN) return
        if (event.keyCode != key) return
        if (Minecraft.getInstance().screen != null) return
        Minecraft.getInstance().setScreen(PlaytimeTrackerGui())
    }
}
