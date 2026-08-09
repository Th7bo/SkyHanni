package at.hannibal2.skyhanni.features.misc.playtimetracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import java.time.LocalDate

/**
 * Shows stored per-day SkyBlock seconds, today, rolling average, and scrollable history.
 * Clicking a day expands it into the per-island breakdown of that day.
 */
class PlaytimeTrackerGui : SkyHanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.misc.dailyPlaytime

    private var scroll = 0.0
    private var expandedDate: String? = null
    private val w = 380
    private val h = 300

    /** One line of the scrollable list. [dateKey] is set for clickable day rows only. */
    private data class ListRow(val text: String, val dateKey: String? = null)

    override fun onInitGui() {
        scroll = 0.0
    }

    private fun headerOffset() = if (config.showMaxPlaytime) 11 else 0

    private fun innerTop() = (height / 2 - h / 2) + 64 + headerOffset()

    private fun innerBottom() = (height / 2 - h / 2) + h - 6

    private fun buildRows(): List<ListRow> {
        val stor = ProfileStorageData.playerSpecific?.skyblockDailyPlaytime
        val days = stor?.secondsByIsoDate.orEmpty().entries.mapNotNull { (key, secs) ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
            date to secs
        }.sortedByDescending { it.first }

        return buildList {
            for ((date, secs) in days) {
                val iso = date.toString()
                val expanded = iso == expandedDate
                val marker = if (expanded) "§8[-]" else "§8[+]"
                add(
                    ListRow(
                        "§f$iso §7│ §f${SkyblockDailyPlaytimeTracker.formatSeconds(secs)} $marker",
                        dateKey = iso,
                    ),
                )
                if (expanded) addAll(breakdownRows(iso, secs))
            }
        }
    }

    private fun breakdownRows(iso: String, daySeconds: Long): List<ListRow> {
        val breakdown = SkyblockDailyPlaytimeTracker.islandBreakdownFor(iso)
        val untracked = SkyblockDailyPlaytimeTracker.untrackedSecondsFor(iso)
        if (breakdown.isEmpty() && untracked == daySeconds) {
            return listOf(ListRow("   §8No island data recorded for this day."))
        }
        return buildList {
            for ((island, secs) in breakdown) {
                val time = SkyblockDailyPlaytimeTracker.formatSeconds(secs)
                add(ListRow("   §7• §f$island §7— §f$time${percent(secs, daySeconds)}"))
            }
            if (untracked > 0) {
                val time = SkyblockDailyPlaytimeTracker.formatSeconds(untracked)
                add(ListRow("   §7• §8Unknown §7— §7$time${percent(untracked, daySeconds)}"))
            }
        }
    }

    private fun percent(part: Long, total: Long): String {
        if (total <= 0L) return ""
        return " §8(${(part * 100.0 / total).toInt()}%)"
    }

    private fun contentHeight(rows: List<ListRow>): Int = if (rows.isEmpty()) 14 else 11 + rows.size * 10

    private fun maxScroll(rows: List<ListRow>): Double =
        (contentHeight(rows) - (innerBottom() - innerTop())).coerceAtLeast(0).toDouble()

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

        val rows = buildRows()
        val innerTop = innerTop()
        val innerBottom = innerBottom()
        scroll = scroll.coerceIn(0.0, maxScroll(rows))

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
            if (config.showMaxPlaytime) {
                yHeader += 11
                val (maxSecs, maxDate) = SkyblockDailyPlaytimeTracker.computeAllTimeMax()
                val dateSuffix = if (maxDate != null) " §7($maxDate)" else ""
                GuiRenderUtils.drawString(
                    "§eAll-time max: §f${SkyblockDailyPlaytimeTracker.formatSeconds(maxSecs)}$dateSuffix",
                    8,
                    yHeader,
                    -1,
                )
            }

            GuiRenderUtils.drawRect(8, 56 + headerOffset(), w - 8, 57 + headerOffset(), 0x80404040.toInt())

            DrawContextUtils.translate(-l.toFloat(), -t.toFloat())
            GuiRenderUtils.enableScissor(l + 5, innerTop, l + w - 5, innerBottom)
            DrawContextUtils.translate(l.toFloat(), t.toFloat())
            DrawContextUtils.translate(8f, ((innerTop - t) - scroll).toFloat())

            if (rows.isEmpty()) {
                GuiRenderUtils.drawString("§7No history yet. Enable tracking and spend time in SkyBlock.", 0, 0, -1)
            } else {
                GuiRenderUtils.drawString("§7Date         Time played §8(click a day for islands)", 0, 0, -1)
                var rowY = 11
                for (row in rows) {
                    if (row.dateKey != null && row.dateKey == expandedDate) {
                        GuiRenderUtils.drawRect(-3, rowY - 1, w - 21, rowY + 9, 0x40FFFFFF)
                    }
                    GuiRenderUtils.drawString(row.text, 0, rowY, -1)
                    rowY += 10
                }
            }

            GuiRenderUtils.disableScissor()
        }
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        val l = width / 2 - w / 2
        if (mouseX < l + 5 || mouseX > l + w - 5) return
        val innerTop = innerTop()
        if (mouseY < innerTop || mouseY > innerBottom()) return

        val rows = buildRows()
        // Content coordinates, skipping the column header line at the top of the list.
        val contentY = mouseY - innerTop + scroll - 11
        if (contentY < 0) return
        val clicked = rows.getOrNull((contentY / 10).toInt()) ?: return
        val dateKey = clicked.dateKey ?: return
        expandedDate = if (expandedDate == dateKey) null else dateKey
        scroll = scroll.coerceIn(0.0, maxScroll(buildRows()))
    }

    override fun onHandleMouseInput() {
        scroll = (scroll - MouseCompat.getScrollDelta()).coerceIn(0.0, maxScroll(buildRows()))
    }

    override fun isPauseScreen() = false
}
