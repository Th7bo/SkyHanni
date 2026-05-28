package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
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

    private val DIVAN_ALLOY = "DIVAN_ALLOY".toInternalName()
    private val olivemanTexture = createResourceLocation("skyhanni", "textures/gui/easteregg/oliveman.png")
    private val chompsSound by lazy { SoundUtils.createSound("skyhanni:easteregg.chomps", 1f, 2f) }

    // Easter egg 1: VideoPlayer loaded at startup; play() is called when Divan Alloy drops
    private val divanReelPlayer = VideoPlayer("video/brainrot_reel.gif", loop = false)

    private var fishImageShowUntil = SimpleTimeMark.farPast()
    private val DISPLAY_DURATION = 5.seconds
    private val FADE_DURATION = 1.seconds

    // Easter egg 1: play brainrot insta reel when Divan Alloy drops from a Crystal Nucleus run
    @HandleEvent(onlyOnSkyblock = true)
    fun onNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!event.loot.containsKey(DIVAN_ALLOY)) return
        divanReelPlayer.play()
    }

    // Easter egg 2: show oliveman.png when a diamond trophy fish/frog is caught
    @HandleEvent(onlyOnSkyblock = true)
    fun onTrophyFishCaught(event: TrophyFishCaughtEvent) {
        if (event.rarity != TrophyRarity.DIAMOND) return
        fishImageShowUntil = SimpleTimeMark.now() + DISPLAY_DURATION
    }

    // Easter egg 3: play chomps.ogg when a God Potion is consumed
    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        godPotConsumePattern.matchMatcher(event.message) {
            chompsSound.playSound()
        }
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        divanReelPlayer.renderFullscreen()
        if (!fishImageShowUntil.isInFuture()) return
        val timeLeft = fishImageShowUntil.timeUntil()
        val alpha = (timeLeft / FADE_DURATION).toFloat().coerceIn(0f, 1f)
        GuiRenderUtils.drawTexturedRect(0f, 0f, olivemanTexture, alpha)
    }
}
