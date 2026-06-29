package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.SkyBlockXPApi
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen

/**
 * Shows every SkyBlock Level color tier of the active palette with its level range drawn in that color.
 */
class LevelColorGui : SkyHanniBaseScreen() {

    private val boxWidth = 360
    private val boxHeight = 232
    private val rowHeight = 14
    private val columnSplit = 13 // base tiers 0-519 on the left, the new endgame ramp on the right

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        val left = width / 2 - boxWidth / 2
        val top = height / 2 - boxHeight / 2
        val paletteName = when {
            SkyBlockXPApi.usingRainbow -> "Rainbow (per level)"
            SkyBlockXPApi.usingRevampedPalette -> "Revamped"
            else -> "Default"
        }

        DrawContextUtils.translatedPushPopResult(x = left.toDouble(), y = top.toDouble()) {
            GuiRenderUtils.drawFloatingRectDark(0, 0, boxWidth, boxHeight)

            GuiRenderUtils.drawString("§b§lSkyBlock Level Colors", 12, 10, -1)
            GuiRenderUtils.drawString("§7Palette: §f$paletteName §7(toggle in §eGUI ▸ SkyBlock Level Colors§7)", 12, 24, -1)

            if (SkyBlockXPApi.usingRainbow) drawRainbow() else drawTiers()
        }
    }

    private fun drawTiers() {
        SkyBlockXPApi.getLevelColorTiers().forEachIndexed { index, (range, rgb) ->
            val column = if (index < columnSplit) 0 else 1
            val rowInColumn = if (column == 0) index else index - columnSplit
            val x = 12 + column * 180
            val y = 46 + rowInColumn * rowHeight
            drawTier(x, y, range, rgb)
        }
    }

    private fun drawTier(x: Int, y: Int, range: IntRange, rgb: Int) {
        val color = rgb or 0xFF000000.toInt()
        GuiRenderUtils.drawRect(x, y, x + 12, y + 9, color)
        GuiRenderUtils.drawRect(x, y, x + 12, y + 1, 0xFF000000.toInt())
        val label = if (range.last == Int.MAX_VALUE) "${range.first}+" else "${range.first} - ${range.last}"
        GuiRenderUtils.drawString("Level $label", x + 18, y, color)
    }

    private fun drawRainbow() {
        val barLeft = 12
        val barTop = 50
        val barWidth = boxWidth - 24
        val barHeight = 40
        val maxLevel = 480

        // Continuous gradient: one vertical line per pixel, sampling the per-level rainbow.
        for (i in 0 until barWidth) {
            val level = i * maxLevel / barWidth
            val color = SkyBlockXPApi.getLevelColorRgb(level) or 0xFF000000.toInt()
            GuiRenderUtils.drawRect(barLeft + i, barTop, barLeft + i + 1, barTop + barHeight, color)
        }

        // Sample labels below the bar, each drawn in its own level color.
        var y = barTop + barHeight + 12
        for (level in 0..maxLevel step 80) {
            val color = SkyBlockXPApi.getLevelColorRgb(level) or 0xFF000000.toInt()
            GuiRenderUtils.drawString("Level $level", 12, y, color)
            y += rowHeight
        }
    }
}
