package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.features.misc.RareDropAnimationConfig.AnimationStyle
import at.hannibal2.skyhanni.config.features.misc.RareDropAnimationConfig.IgnoreModeEntry
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object RareDropAnimation {

    private val config get() = SkyHanniMod.feature.misc.rareDropAnimation
    private val repoGroup = RepoPattern.group("misc.raredropanimation")

    /**
     * REGEX-TEST: §r§6§lRARE DROP! §r§6§lEnchanted Book §r§b(+208% ✯ Magic Find)
     * REGEX-TEST: §r§6§lRARE DROP! §r§fWither Cloak Sword
     * REGEX-TEST: §r§6§lRARE DROP! §r§5Tarantula Talisman §r§b(+100% ✯ Magic Find)
     * REGEX-TEST: §r§6§lRARE DROP! §r§6§lEnchanted Hay Bale x3 §r§b(+94.5☀)
     * REGEX-TEST: §r§6§lRARE CROP! §r§6§lCropie §r§b(+39.5☀)
     */
    private val rareDropPattern by repoGroup.pattern(
        "raredrop",
        "(?:§.)*RARE (?:DROP|CROP)! (?:§.)*(?<item>[^(§\n]+?)(?:\\s*x\\d+)?\\s*(?:(?:§.)*\\(.*)?$",
    )

    /**
     * REGEX-TEST: §6§lPET DROP! §r§5Baby Yeti §r§b(+168% ✯ Magic Find)
     * REGEX-TEST: §6§lPET DROP! §r§6Rat
     */
    private val petDropPattern by repoGroup.pattern(
        "petdrop",
        "(?:§.)*PET DROP! (?:§.)*§(?<rarityColor>.)(?<item>[^§(\n]+?)\\s*(?:(?:§.)*\\(.*)?$",
    )

    /**
     * REGEX-TEST: §6⛃ §r§6§lGREAT CATCH! §r§fYou caught a §r§7[Lvl 1] §r§5Giraffe§r§f!
     * REGEX-TEST: §6⛃ §r§6§lOUTSTANDING CATCH! §r§fYou caught a §r§7[Lvl 1] §r§6Bal§r§f!
     */
    private val fishingCatchPattern by repoGroup.pattern(
        "fishingcatch",
        "(?:§.)*⛃ (?:§.)*(?:GOOD|GREAT|OUTSTANDING) CATCH! (?:§.)*You caught a (?:§.)*\\[Lvl 1] (?:§.)*§(?<rarityColor>.)(?<pet>[^§!\n]+?)(?:§.*)?!",
    )

    private var animationStart = SimpleTimeMark.farPast()
    private var currentItem: ItemStack? = null
    private var currentRarity: LorenzRarity? = null
    private var currentItemName: String = ""
    private var currentPrice: Double? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return
        val message = event.message

        rareDropPattern.matchMatcher(message) {
            val itemName = group("item").trim().removeColor().replace(Regex(" x\\d+$"), "")
            val internalName = NeuInternalName.fromItemNameOrNull(itemName) ?: return
            val itemStack = internalName.getItemStackOrNull() ?: return
            if (isSuppressed(itemStack, internalName)) return
            val rarity = itemStack.getItemRarityOrNull()
            triggerAnimation(itemStack, internalName, rarity, itemName)
            return
        }

        if (config.includePetDrops) {
            petDropPattern.matchMatcher(message) {
                val rarityChar = group("rarityColor").first()
                val rarity = LorenzRarity.getByColorCode(rarityChar)
                val petName = group("item").trim().removeColor()
                val (itemStack, internalName) = resolvePetStack(petName, rarity) ?: return
                if (isSuppressed(itemStack, internalName)) return
                triggerAnimation(itemStack, internalName, rarity, petName)
                return
            }
        }

        if (config.includeTrophyFish) {
            fishingCatchPattern.matchMatcher(message) {
                val rarityChar = group("rarityColor").first()
                val rarity = LorenzRarity.getByColorCode(rarityChar)
                val petName = group("pet").trim().removeColor()
                val (itemStack, internalName) = resolvePetStack(petName, rarity) ?: return
                if (isSuppressed(itemStack, internalName)) return
                triggerAnimation(itemStack, internalName, rarity, petName)
            }
        }
    }

    private fun resolvePetStack(petName: String, rarity: LorenzRarity?): Pair<ItemStack, NeuInternalName>? {
        if (rarity != null) {
            val name = PetUtils.petWithRarityToInternalName(petName, rarity)
            name.getItemStackOrNull()?.let { return it to name }
        }
        val fallback = NeuInternalName.fromItemNameOrNull("$petName Pet") ?: return null
        return fallback.getItemStackOrNull()?.let { it to fallback }
    }

    private fun isSuppressed(itemStack: ItemStack, internalName: NeuInternalName): Boolean {
        val ignoredIslands = config.ignoredLocations.flatMap { it.islands }
        if (ignoredIslands.any { it.isInIsland() }) return true

        val key = internalName.asString()
        if (key in config.customIgnoredItems) return true

        if (config.ignoreMode == IgnoreModeEntry.CATEGORY) {
            val category = itemStack.getItemCategoryOrNull()
            if (category != null && category.name in config.customIgnoredItems) return true
        }

        return false
    }

    private fun triggerAnimation(
        item: ItemStack,
        internalName: NeuInternalName,
        rarity: LorenzRarity?,
        name: String,
    ) {
        if (config.playSound) SoundUtils.plingSound.playSound()
        if (config.showIgnoreHint) sendIgnoreHint(internalName, rarity, name)
        if (config.animationStyle == AnimationStyle.VANILLA) {
            Minecraft.getInstance().gameRenderer.displayItemActivation(item)
        } else {
            currentItem = item
            currentRarity = rarity
            currentItemName = name
            currentPrice = internalName.getPriceOrNull()
            animationStart = SimpleTimeMark.now()
        }
    }

    private fun sendIgnoreHint(internalName: NeuInternalName, rarity: LorenzRarity?, name: String) {
        val colorCode = rarity?.color?.getChatColor() ?: "§f"
        val displayName = "$colorCode$name"
        val key = when (config.ignoreMode) {
            IgnoreModeEntry.SPECIFIC_ITEM -> internalName.asString()
            IgnoreModeEntry.CATEGORY -> {
                internalName.getItemStackOrNull()?.getItemCategoryOrNull()?.name ?: internalName.asString()
            }
        }
        ChatUtils.clickableChat(
            "§8[§c§lIgnore§8] §7Click to stop showing the animation for $displayName",
            onClick = {
                if (key !in config.customIgnoredItems) {
                    config.customIgnoredItems.add(key)
                    SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "rare drop animation ignore list")
                    ChatUtils.chat("§7Added $displayName §7to your ignore list.")
                } else {
                    ChatUtils.chat("$displayName §7is already ignored.")
                }
            },
            hover = "§7Ignore $displayName",
            prefix = false,
            oneTimeClick = true,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (config.animationStyle == AnimationStyle.VANILLA) return
        val item = currentItem ?: return
        val duration = config.duration.toDouble().seconds
        val elapsed = animationStart.passedSince()
        if (elapsed > duration) return

        val progress = elapsed.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()
        val alpha = when {
            progress < 0.1f -> progress / 0.1f
            progress > 0.8f -> 1f - (progress - 0.8f) / 0.2f
            else -> 1f
        }.coerceIn(0f, 1f)

        val screenW = GuiScreenUtils.scaledWindowWidth
        val screenH = GuiScreenUtils.scaledWindowHeight
        val centerX = screenW / 2f
        val centerY = screenH * 0.42f

        renderFlash(screenW, screenH, progress, alpha)
        if (config.showItemIcon) renderItem(item, centerX, centerY, progress, alpha)
        if (config.showItemName) renderItemName(centerX, centerY, alpha)
        if (config.showItemPrice) renderItemPrice(centerX, centerY, alpha)
    }

    private fun renderFlash(screenW: Int, screenH: Int, progress: Float, alpha: Float) {
        if (!config.showFlash) return
        val flashAlpha = when {
            progress < 0.1f -> progress / 0.1f
            progress < 0.4f -> 1f - (progress - 0.1f) / 0.3f
            else -> 0f
        } * 0.45f * alpha
        if (flashAlpha <= 0.01f) return
        val javaColor = (currentRarity ?: LorenzRarity.LEGENDARY).color.toColor()
        val flashColor = ARGB.color(
            (flashAlpha * 255).toInt().coerceIn(0, 255),
            javaColor.red,
            javaColor.green,
            javaColor.blue,
        )
        DrawContextUtils.drawContext.fill(0, 0, screenW, screenH, flashColor)
    }

    private fun renderItem(item: ItemStack, centerX: Float, centerY: Float, progress: Float, alpha: Float) {
        val scale = config.itemScale.toDouble()
        val halfSize = (8 * scale).toFloat()
        val bob = sin(progress * Math.PI * 6).toFloat() * 3f * (1f - progress)
        item.renderOnScreen(
            x = centerX - halfSize,
            y = centerY - halfSize + bob,
            scale = scale,
            alpha = alpha,
        )
    }

    private fun renderItemName(centerX: Float, centerY: Float, alpha: Float) {
        if (currentItemName.isEmpty()) return
        val scale = config.itemScale.toDouble()
        val halfSize = (8 * scale).toFloat()
        val colorCode = (currentRarity?.color?.getChatColor() ?: "§6") + "§l"
        val displayName = "$colorCode$currentItemName"
        val textY = centerY + halfSize + 6f
        val textScale = 1.8f
        val fr = Minecraft.getInstance().font
        val textAlpha = (alpha * 255).toInt().coerceIn(0, 255)

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(centerX.toDouble(), textY.toDouble())
            DrawContextUtils.scale(textScale, textScale)
            val textWidth = fr.width(displayName)
            DrawContextUtils.drawContext.drawString(
                fr,
                displayName,
                -(textWidth / 2),
                0,
                ARGB.color(textAlpha, 255, 255, 255),
                true,
            )
        }
    }

    private fun renderItemPrice(centerX: Float, centerY: Float, alpha: Float) {
        val price = currentPrice ?: return
        if (price <= 0) return
        val scale = config.itemScale.toDouble()
        val halfSize = (8 * scale).toFloat()
        val textScale = 1.8f
        val fr = Minecraft.getInstance().font
        val nameLineHeight = textScale * fr.lineHeight
        val nameOffset = if (config.showItemName && currentItemName.isNotEmpty()) nameLineHeight else 0f
        val textY = centerY + halfSize + 6f + nameOffset
        val formatted = price.formatCoin()
        val textAlpha = (alpha * 255).toInt().coerceIn(0, 255)

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(centerX.toDouble(), textY.toDouble())
            DrawContextUtils.scale(textScale, textScale)
            val textWidth = fr.width(formatted)
            DrawContextUtils.drawContext.drawString(
                fr,
                formatted,
                -(textWidth / 2),
                0,
                ARGB.color(textAlpha, 255, 255, 255),
                true,
            )
        }
    }
}
