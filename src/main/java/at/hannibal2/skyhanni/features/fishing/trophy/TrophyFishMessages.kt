package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getTooltip
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TrophyFishMessages {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.chatMessages

    /**
     * REGEX-TEST: §6 §r§6§lTROPHY FISH! §r§fYou caught a §r§9Lavahorse §r§6§lGOLD§r§f!
     * REGEX-TEST: §6 §r§6§lTROPHY FISH! §r§fYou caught a §r§5Soul Fish §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6 §r§6§lTROPHY FISH! §r§fYou caught a §r§9Mana Ray §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6 §r§6§lTROPHY FISH! §r§fYou caught a §r§fBlobfish §r§7§lSILVER§r§f!
     * REGEX-TEST: §6 §r§6§lTROPHY FISH! §r§fYou caught a §r§6Golden Fish §r§7§lSILVER§r§f!
     */
    @Suppress("MaxLineLength")
    val trophyFishPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfish",
        "§6\uE02A §r§6§lTROPHY FISH! §r§fYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+) §r(?<displayRarity>§[0-9a-f]§l\\w+)§r§f!",
    )

    /**
     * Personal trophy frog catch (every catch except the first DIAMOND).
     * REGEX-TEST: §2♔ §r§2§lTROPHY FROG! §r§fYou caught an §r§aExploding Frog §r§7§lSILVER§r§f!
     * REGEX-TEST: §2♔ §r§2§lTROPHY FROG! §r§fYou caught a §r§fCommon Frog §r§b§lDIAMOND§r§f!
     */
    @Suppress("MaxLineLength")
    val trophyFrogPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfrog",
        "§2♔ §r§2§lTROPHY FROG! §r§fYou caught an? §r(?<displayName>§[0-9a-f](?:§k)?[\\w -]+) §r(?<displayRarity>§[0-9a-f]§l\\w+)§r§f!",
    )

    /**
     * Global broadcast on first DIAMOND frog catch (no personal message in that case).
     * REGEX-TEST: §2§lRIBBIT! §r§7§r§b[MVP§r§a+§r§b] Th7bo§r§f§r§e caught their first §r§b§lDIAMOND §r§fCommon Frog§r§e!
     * REGEX-TEST: §2§lRIBBIT! §r§7§r§b[MVP§r§9+§r§b] TTC_cure§r§f§r§e caught their first §r§b§lDIAMOND §r§fCommon Frog§r§e!
     */
    val trophyFrogFirstCatchPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfrog.firstcatch",
        "(?:§.)*RIBBIT! .*?(?<playerName>\\w+)§r(?:§.)+ caught their first (?:§.)+(?<displayRarity>\\w+) (?:§.)+(?<displayName>[\\w ]+?)§r(?:§.)+!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val (displayName, displayRarity) =
            trophyFishPattern.matchMatcher(event.message) {
                group("displayName").replace("§k", "") to group("displayRarity")
            } ?: trophyFrogPattern.matchMatcher(event.message) {
                group("displayName").replace("§k", "") to group("displayRarity")
            } ?: return

        val internalName = TrophyFishApi.getInternalName(displayName)
        val rarity = TrophyRarity.getByName(displayRarity.lowercase().removeColor()) ?: return

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, 1)
        TrophyFishCaughtEvent(internalName, rarity).post()

        if (shouldBlockTrophyFish(rarity, amount)) {
            event.blockedReason = "low_trophy_fish"
            return
        }

        if (config.duplicateHider) event.chatLineId = (internalName + rarity).hashCode()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onFrogFirstCatchChat(event: SkyHanniChatEvent.Allow) {
        trophyFrogFirstCatchPattern.matchMatcher(event.message) {
            if (group("playerName") != PlayerUtils.getName()) return
            val rarity = TrophyRarity.getByName(group("displayRarity").lowercase()) ?: return
            val internalName = TrophyFishApi.getInternalName(group("displayName"))
            TrophyFishCaughtEvent(internalName, rarity).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        val (displayName, displayRarity) = trophyFishPattern.matchMatcher(event.message) {
            group("displayName").replace("§k", "") to
                group("displayRarity")
        } ?: return

        val internalName = TrophyFishApi.getInternalName(displayName)
        val rarity = TrophyRarity.getByName(displayRarity.lowercase().removeColor()) ?: return

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts[rarity] ?: 1

        if (config.goldAlert && rarity == TrophyRarity.GOLD) {
            sendTitle(displayName, displayRarity, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        if (config.diamondAlert && rarity == TrophyRarity.DIAMOND) {
            sendTitle(displayName, displayRarity, amount)
            if (config.playSound) SoundUtils.playBeepSound()
        }

        val edited = if (config.enabled) {
            val designFormat = when (config.design) {
                DesignFormat.STYLE_1 -> if (amount == 1) "§c§lFIRST §r$displayRarity $displayName"
                else "§7$amount${amount.ordinal()} §r$displayRarity $displayName"

                DesignFormat.STYLE_2 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
                else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
            }
            "§6${SkyblockStat.TROPHY_FISH_CHANCE.icon} §6§lTROPHY FISH! $designFormat".asComponent()
        } else event.chatComponent.copy()

        if (config.totalAmount) {
            val total = trophyFishCounts.sumAllValues()
            edited.append((" §7(${total.addSeparators()}${total.ordinal()} total)"))
        }

        if (config.tooltip) {
            getTooltip(internalName)?.let {
                edited.toFlatList(it)
            }
        }

        event.replaceComponent(edited, "TROPHY_FISH")
    }

    private fun sendTitle(displayName: String, displayRarity: String?, amount: Int) {
        val text = "$displayName $displayRarity §8$amount§c!"
        TitleManager.sendTitle(text)
    }

    private fun shouldBlockTrophyFish(rarity: TrophyRarity, amount: Int) =
        config.bronzeHider &&
            rarity == TrophyRarity.BRONZE &&
            amount != 1 ||
            config.silverHider &&
            rarity == TrophyRarity.SILVER &&
            amount != 1

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyCounter", "fishing.trophyFishing.chatMessages.enabled")
        event.move(2, "fishing.trophyDesign", "fishing.trophyFishing.chatMessages.design")
        event.move(2, "fishing.trophyFishTotalAmount", "fishing.trophyFishing.chatMessages.totalAmount")
        event.move(2, "fishing.trophyFishTooltip", "fishing.trophyFishing.chatMessages.tooltip")
        event.move(2, "fishing.trophyFishDuplicateHider", "fishing.trophyFishing.chatMessages.duplicateHider")
        event.move(2, "fishing.trophyFishBronzeHider", "fishing.trophyFishing.chatMessages.bronzeHider")
        event.move(2, "fishing.trophyFishSilverHider", "fishing.trophyFishing.chatMessages.silverHider")
    }
}
