package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.ColorUtils.isChroma
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.insert
import at.hannibal2.skyhanni.utils.StringUtils.splitCamelCase
import at.hannibal2.skyhanni.utils.compat.getDoubleOrDefault
import at.hannibal2.skyhanni.utils.compat.withColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor

private val PROMISING_SHOVEL = "PROMISING_SHOVEL".toInternalName()
private val STONK_PICKAXE = "STONK_PICKAXE".toInternalName()

open class Enchant : Comparable<Enchant> {

    // TODO move this away. split json data from logic
    @Expose
    var nbtName = ""

    @Expose
    var loreName = ""

    @Expose
    private val goodLevel = 0

    @Expose
    private val maxLevel = 0

    private fun isNormal() = this is Normal
    private fun isUltimate() = this is Ultimate
    private fun isStacking() = this is Stacking

    val config by lazy { SkyHanniMod.feature.inventory.enchantParsing }

    open fun getComponent(level: Int, itemStack: SafeItemStack?, isRoman: Boolean, appendNewline: Boolean = false): Component {
        val text = "$loreName ${if (isRoman) level.toRoman() else level}${if (appendNewline) "\n" else ""}"
        return Component.literal(text).setStyle(getStyle(level, itemStack))
    }

    open fun getStyle(level: Int, itemStack: SafeItemStack? = null): Style {
        val colorProperty: Property<ChromaColour> = when {
            level >= maxLevel -> config.perfectEnchantColor
            level > goodLevel -> config.greatEnchantColor
            level == goodLevel -> config.goodEnchantColor
            else -> config.poorEnchantColor
        }

        // Exceptions
        checkExceptions(level, itemStack)?.let { return it }

        var style = colorProperty.get().toEnchantStyle()

        if (level >= maxLevel && config.boldPerfectEnchant.get()) style = style.withBold(true)

        return style
    }

    /**
     * Converts a configured color into the style used to render an enchant.
     *
     * Static colors use their plain RGB value, while chroma colors are routed through SkyHanni's
     * chroma shader (via the special "chroma" [TextColor]), falling back to bold gold when chroma
     * rendering is unavailable.
     */
    protected fun ChromaColour.toEnchantStyle(): Style {
        if (!isChroma()) {
            return Style.EMPTY.withColor(getEffectiveColourRGB())
        }
        if (!(ChromaManager.config.enabled.get() || EnchantParser.isSbaLoaded)) {
            return Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
        }
        return Style.EMPTY.withColor(TextColor(0xFFFFFF, "chroma"))
    }

    /**
     * Method to check for certain or unique exceptions that need to be handled explicitly.
     *
     * *(There isn't much of a convention to adding exceptions, except try to include relevant exceptions under
     * a corresponding enchantment conditional, unless the exception is not specific to a certain enchant, e.g.
     * Efficiency exceptions should be within the `"efficiency"` conditional.)*
     *
     * @param level The level of the enchant currently being parsed
     * @param itemStack The SafeItemStack of the hovered item. Can be null, e.g. when hovering over `/show` items
     */
    private fun checkExceptions(level: Int, itemStack: SafeItemStack?): Style? {
        val internalName = itemStack?.getInternalNameOrNull()
        val itemCategory = itemStack?.getItemCategoryOrNull()

        return when (nbtName) {
            "efficiency" -> {
                // If the item is a Stonk, or a non-mining tool with Efficiency 5
                // (whilst not being a Promising Shovel), color the enchant as max
                if (internalName == STONK_PICKAXE ||
                    (level == 5 &&
                        itemCategory !in ItemCategory.miningTools &&
                        internalName != PROMISING_SHOVEL)
                ) {
                    config.perfectEnchantColor.get().toEnchantStyle()
                        .let { if (config.boldPerfectEnchant.get()) it.withBold(true) else it }
                } else null
            }
            else -> null
        }
    }

    override fun toString() = "$nbtName $goodLevel $maxLevel\n"

    override fun compareTo(other: Enchant): Int {
        if (isUltimate() == other.isUltimate()) {
            if (isStacking() == other.isStacking()) {
                return loreName.compareTo(other.loreName)
            }
            return if (isStacking()) -1 else 1
        }
        return if (isUltimate()) -1 else 1
    }

    class Normal : Enchant()

    class Ultimate : Enchant() {
        override fun getStyle(level: Int, itemStack: SafeItemStack?): Style {
            return config.ultimateEnchantColor.get().toEnchantStyle().withBold(true)
        }
    }

    class Stacking : Enchant() {
        @Expose
        val nbtNum: String = ""

        @Expose
        private val statLabel: String = ""

        @Expose
        val stackLevel: List<Int> = emptyList()

        override fun toString() = "$nbtNum $stackLevel ${super.toString()}"

        fun progressString(item: SafeItemStack): String {
            val label = statLabel.splitCamelCase().replaceFirstChar { it.uppercase() }.replace("Xp", "XP")
            val progress = item.extraAttributes.getDoubleOrDefault(nbtNum).roundTo(0).toInt()
            if (progress == 0) return ""
            val nextLevel = stackLevel.filter { it > progress }.minOrNull()
            val tail = nextLevel?.shortFormat()?.insert(0, "/ ") ?: "(Maxed)"
            return "§7$label: §c${progress.shortFormat()} §7$tail"
        }
    }

    class Dummy(name: String) : Enchant() {
        init {
            loreName = name
            nbtName = name
        }

        // Ensures enchants not yet in repo stay as vanilla formatting
        // (instead of that stupid dark red lowercase formatting *cough* sba *cough*)
        override fun getComponent(level: Int, itemStack: SafeItemStack?, isRoman: Boolean, appendNewline: Boolean): Component {
            val text = "$loreName ${if (isRoman) level.toRoman() else level}${if (appendNewline) "\n" else ""}"
            return Component.literal(text).withColor(ChatFormatting.BLUE)
        }
    }
}
