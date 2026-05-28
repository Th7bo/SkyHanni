package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.slayer.SlayerDeathEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.VideoPlayer
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object EasterEggs {

    private val patternGroup = RepoPattern.group("misc.easteregg")

    /**
     * REGEX-TEST: §a§lGULP! §r§eThe §r§cGod Potion §r§egrants you powers for §r§928h 48m§r§e!
     */
    private val godPotConsumePattern by patternGroup.pattern(
        "godpot",
        "(?:§.)+.*(?:§.)+God Potion ?(?:§.)+.*grants you powers for (?:§.)+(?:[dhms0-9 ]*)(?:§.)+!.*",
    )

    private val insaneDropPattern by patternGroup.pattern(
        "insane.drop",
        "(?:§.)*INSANE DROP! (?:§.)*\\((?:§.)*(?<item>[^)]+)\\) (?:§.)*\\(\\+(?<magicfind>\\d+)% (?:§.)*. Magic Find\\)",
    )

    private val DIVAN_ALLOY = "DIVAN_ALLOY".toInternalName()
    private val chompsSound by lazy { SoundUtils.createSound("skyhanni:easteregg.chomps", 1f, 2f) }

    // Easter egg 1: VideoPlayer loaded at startup; play() is called when Divan Alloy drops
    private val divanReelPlayer = VideoPlayer("video/brainrot_reel.gif", loop = false)

    private enum class EasterEgg(val displayName: String) {
        DIVAN_REEL("Brainrot reel on Divan Alloy drop"),
        OLIVEMAN("Oliveman image on Diamond Trophy catch"),
        PIZZAMAN("Pizzaman image on Insane Drop"),
        CHOMPS("Chomps sound on God Potion consume"),
    }

    private enum class ImageType {
        OLIVEMAN, PIZZAMAN, INACTIVE;

        private val texture by lazy { createResourceLocation("skyhanni", "textures/gui/easteregg/${name.lowercase()}.png") }
        fun getTextures() = texture
    }

    private var showUntil = SimpleTimeMark.farPast()
    private val DISPLAY_DURATION = 5.seconds
    private val FADE_DURATION = 1.seconds
    private var ACTIVE_IMAGE = ImageType.INACTIVE

    private fun trigger(egg: EasterEgg, action: () -> Unit) {
        val storage = ProfileStorageData.playerSpecific?.easterEggs ?: return
        if (egg.name in storage.disabled) return
        action()
        if (storage.seen.add(egg.name)) return
        ChatUtils.clickableChat(
            "§eClick here to disable this easter egg.",
            hover = "§7Disables: §f${egg.displayName}",
            onClick = {
                storage.disabled.add(egg.name)
                ChatUtils.chat("§7Disabled easter egg: §f${egg.displayName}")
            },
            oneTimeClick = true,
        )
    }

    // Easter egg 1: play brainrot insta reel when Divan Alloy drops from a Crystal Nucleus run
    @HandleEvent(onlyOnSkyblock = true)
    fun onNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!event.loot.containsKey(DIVAN_ALLOY)) return
        trigger(EasterEgg.DIVAN_REEL) { divanReelPlayer.play() }
    }

    // Easter egg 2: show oliveman.png when a diamond trophy fish/frog is caught
    @HandleEvent(onlyOnSkyblock = true)
    fun onTrophyFishCaught(event: TrophyFishCaughtEvent) {
        if (event.rarity != TrophyRarity.DIAMOND) return
        trigger(EasterEgg.OLIVEMAN) {
            ACTIVE_IMAGE = ImageType.OLIVEMAN
            showUntil = SimpleTimeMark.now() + DISPLAY_DURATION
        }
    }

    // Easter egg 3: play chomps.ogg when a God Potion is consumed
    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        godPotConsumePattern.matchMatcher(event.message) {
            trigger(EasterEgg.CHOMPS) { chompsSound.playSound() }
        }

        // Easter egg 4: show pizzaman.png when an Insane Drop is found
        insaneDropPattern.matchMatcher(event.message) {
            trigger(EasterEgg.PIZZAMAN) {
                ACTIVE_IMAGE = ImageType.PIZZAMAN
                showUntil = SimpleTimeMark.now() + DISPLAY_DURATION
            }
        }
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        divanReelPlayer.renderFullscreen()
        if (!showUntil.isInFuture()) return
        if (ACTIVE_IMAGE == ImageType.INACTIVE) return
        val timeLeft = showUntil.timeUntil()
        val alpha = (timeLeft / FADE_DURATION).toFloat().coerceIn(0f, 1f)
        GuiRenderUtils.drawTexturedRect(0f, 0f, ACTIVE_IMAGE.getTextures(), alpha)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("sheasteregg") {
            description = "Test Easter egg triggers."
            category = CommandCategory.DEVELOPER_TEST
            literalCallback("divan") { divanReelPlayer.play(); ChatUtils.chat("Playing Divan reel.") }
            literalCallback("fish") { showUntil = SimpleTimeMark.now() + DISPLAY_DURATION; ACTIVE_IMAGE = ImageType.OLIVEMAN; ChatUtils.chat("Showing oliveman.") }
            literalCallback("pizza") { showUntil = SimpleTimeMark.now() + DISPLAY_DURATION; ACTIVE_IMAGE = ImageType.PIZZAMAN; ChatUtils.chat("Showing pizzaman.") }
            literalCallback("godpot") { chompsSound.playSound(); ChatUtils.chat("Playing chomps.") }
            simpleCallback { ChatUtils.userError("Usage: /sheasteregg <divan|fish|godpot>") }
        }
    }
}
