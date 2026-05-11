package at.hannibal2.skyhanni.features.misc.playtimetracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import java.time.LocalDate

/**
 * Shows stored per-day SkyBlock seconds, today, rolling average, and scrollable history.
 */
class PlaytimeTrackerGui : SkyHanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.misc.dailyPlaytime

    private var scroll = 0.0
    private val w = 380
    private val h = 300

    override fun onInitGui() {
        scroll = 0.0
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        val l = width / 2 - w / 2
        val t = height / 2 - h / 2
        val today = LocalDate.now()
        val windowDays = config.averageWindowDays.toInt().coerceIn(2, 90)
        val stor = ProfileStorageData.playerSpecific?.skyblockDailyPlaytime
        val isoToday = today.toString()
        val todaySecs = stor?.secondsByIsoDate?.get(isoToday) ?: 0L
        val avg = SkyblockDailyPlaytimeTracker.computeRollingAverageDailySeconds(windowDays)

        val sortedRows = stor?.secondsByIsoDate.orEmpty().entries.mapNotNull { (k, secs) ->
            val d = runCatching { LocalDate.parse(k) }.getOrNull() ?: return@mapNotNull null
            d to secs
        }.sortedByDescending { it.first }

        val innerTop = t + 64
        val innerBottom = t + h - 6
        val viewportHeight = innerBottom - innerTop
        val contentHeight =
            if (sortedRows.isEmpty()) 14
            else 11 + sortedRows.size * 10
        val maxScroll = (contentHeight - viewportHeight).coerceAtLeast(0).toDouble()
        scroll = scroll.coerceIn(0.0, maxScroll)

        DrawContextUtils.translatedPushPopResult(x = l.toDouble(), y = t.toDouble()) {
            GuiRenderUtils.drawFloatingRectDark(0, 0, w, h)

            var yHeader = 8
            GuiRenderUtils.drawString("§aSkyBlock daily playtime", 8, yHeader, -1)
            yHeader += 11
            GuiRenderUtils.drawString("§7Local dates · §eToday §7= §f$isoToday", 8, yHeader, -1)
            yHeader += 11
            GuiRenderUtils.drawString(
                "§eToday: §f${SkyblockDailyPlaytimeTracker.formatSeconds(todaySecs)}",
                8,
                yHeader,
                -1,
            )
            yHeader += 11
            GuiRenderUtils.drawString(
                "§eAvg/day §7($windowDays d): §f${SkyblockDailyPlaytimeTracker.formatSeconds((avg.coerceAtLeast(0.0)).toLong())}",
                8,
                yHeader,
                -1,
            )

            GuiRenderUtils.drawRect(8, 56, w - 8, 57, 0x80404040.toInt())

            DrawContextUtils.translate(-l.toFloat(), -t.toFloat())
            GuiRenderUtils.enableScissor(l + 5, innerTop, l + w - 5, innerBottom)
            DrawContextUtils.translate(l.toFloat(), t.toFloat())
            DrawContextUtils.translate(8f, (64 - scroll).toFloat())

            if (sortedRows.isEmpty()) {
                GuiRenderUtils.drawString("§7No history yet. Enable tracking and spend time in SkyBlock.", 0, 0, -1)
            } else {
                GuiRenderUtils.drawString("§7Date         Time played", 0, 0, -1)
                var rowY = 11
                for ((date, secs) in sortedRows) {
                    val dateStr = date.toString()
                    GuiRenderUtils.drawString("§f$dateStr §7│ §f${SkyblockDailyPlaytimeTracker.formatSeconds(secs)}", 0, rowY, -1)
                    rowY += 10
                }
            }

            GuiRenderUtils.disableScissor()
        }
    }

    override fun onHandleMouseInput() {
        val t = height / 2 - h / 2
        val innerTop = t + 64
        val innerBottom = t + h - 6
        val viewportHeight = innerBottom - innerTop
        val stor = ProfileStorageData.playerSpecific?.skyblockDailyPlaytime
        val sortedRows = stor?.secondsByIsoDate.orEmpty().entries.mapNotNull { (k, _) ->
            runCatching { LocalDate.parse(k) }.getOrNull() ?: return@mapNotNull null
        }
        val contentHeight =
            if (sortedRows.isEmpty()) 14
            else 11 + sortedRows.size * 10
        val maxScroll = (contentHeight - viewportHeight).coerceAtLeast(0).toDouble()
        scroll = (scroll - MouseCompat.getScrollDelta()).coerceIn(0.0, maxScroll)
    }

    override fun isPauseScreen() = false
}

