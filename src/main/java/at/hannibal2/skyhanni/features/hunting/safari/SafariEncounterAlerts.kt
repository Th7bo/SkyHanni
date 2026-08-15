package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariBroadcast
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

/**
 * Titles, chat lines and optional party notices for the three staged encounters, plus biome completion.
 *
 * Each encounter announces itself in stages, so each is tracked through ready, started and done:
 *
 * ```
 * GEMZIE (Cavern, chamber)    ready: the door opens          done: all three caught (no end message exists)
 * WUMPA (Icy, on a timer)     ready: massive footsteps       started: it awakes    done: the cave reopens
 * DOOMSPIRAL (Haunted)        ready: a candle is lit         started: it is summoned    done: it retreats
 * ```
 *
 * Catching either boss also counts as done, and the ritual has four candles, so the same stage can be signalled
 * more than once. A per stage cooldown keeps that from firing repeat titles or spamming party chat.
 */
@SkyHanniModule
object SafariEncounterAlerts {

    private val config get() = SkyHanniMod.feature.hunting.safari.alerts
    private val partyConfig get() = SkyHanniMod.feature.hunting.safari.party

    private val patternGroup = RepoPattern.group("hunting.safari.encounters")

    /**
     * REGEX-TEST: A rumbling sound can be heard, and the door to the Gemstone Chamber opens...
     */
    private val gemzieReadyPattern by patternGroup.pattern(
        "gemzie-ready",
        "A rumbling sound can be heard.*",
    )

    /**
     * REGEX-TEST: You hear the sound of massive footsteps in the distance...
     */
    private val wumpaReadyPattern by patternGroup.pattern(
        "wumpa-ready",
        "You hear the sound of massive footsteps.*",
    )

    /**
     * REGEX-TEST: The Wumpa has awoken.
     */
    private val wumpaStartedPattern by patternGroup.pattern(
        "wumpa-started",
        "The Wumpa has awoken.*",
    )

    /**
     * REGEX-TEST: The cave opens up again...
     */
    private val wumpaDonePattern by patternGroup.pattern(
        "wumpa-done",
        "The cave opens up again.*",
    )

    /**
     * REGEX-TEST: You used the Soothing Incense to light the candle! 1/4
     */
    private val doomspiralReadyPattern by patternGroup.pattern(
        "doomspiral-ready",
        "You used the Soothing Incense to light the candle.*",
    )

    /**
     * REGEX-TEST: Your ritual summoned a Doomspiral into this world.
     */
    private val doomspiralStartedPattern by patternGroup.pattern(
        "doomspiral-started",
        "Your ritual summoned a Doomspiral.*",
    )

    /**
     * REGEX-TEST: The Doomspiral retreats back underground...
     */
    private val doomspiralDonePattern by patternGroup.pattern(
        "doomspiral-done",
        "The Doomspiral retreats back underground.*",
    )

    private val stageCooldown = 20.seconds

    /** Exactly this many Gemzies spawn each time the chamber opens. */
    private const val GEMZIE_PER_CHAMBER = 3

    private val lastFired = mutableMapOf<String, SimpleTimeMark>()

    /** Gemzies still to catch in the open chamber; 0 when no chamber is active. */
    private var gemzieRemaining = 0

    /** Lifted while the test command runs, so the biome gate cannot swallow the other two encounters. */
    private var testing = false

    enum class Stage { READY, STARTED, DONE }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        when {
            gemzieReadyPattern.matches(message) -> {
                // Exactly three Gemzies spawn per chamber, so the encounter is over once three have been caught by
                // anyone rather than on any message.
                gemzieRemaining = GEMZIE_PER_CHAMBER
                fire(SafariCritter.GEMZIE, Stage.READY, "chamber open, 3 to catch", 1.4f)
            }
            wumpaReadyPattern.matches(message) -> fire(SafariCritter.WUMPA, Stage.READY, "it wakes in ~30s", 1.4f)
            wumpaStartedPattern.matches(message) -> fire(SafariCritter.WUMPA, Stage.STARTED, "fight is live", 1.8f)
            wumpaDonePattern.matches(message) -> fire(SafariCritter.WUMPA, Stage.DONE, "the cave has reopened", 1.2f)
            doomspiralReadyPattern.matches(message) ->
                fire(SafariCritter.DOOMSPIRAL, Stage.READY, "ritual underway", 1.4f)
            doomspiralStartedPattern.matches(message) ->
                fire(SafariCritter.DOOMSPIRAL, Stage.STARTED, "fight is live", 1.8f)
            doomspiralDonePattern.matches(message) ->
                fire(SafariCritter.DOOMSPIRAL, Stage.DONE, "it retreated", 1.2f)
        }
    }

    /**
     * Called for every catch this run, by anyone.
     *
     * Wumpa and Doomspiral each end when caught. Gemzie has no end message at all, so the count is what closes it.
     */
    fun onCatch(critter: SafariCritter) {
        when (critter) {
            SafariCritter.WUMPA, SafariCritter.DOOMSPIRAL -> fire(critter, Stage.DONE, "caught", 1.2f)
            SafariCritter.GEMZIE -> {
                if (gemzieRemaining <= 0) return
                if (--gemzieRemaining == 0) {
                    fire(critter, Stage.DONE, "all $GEMZIE_PER_CHAMBER caught", 1.2f)
                }
            }
            else -> {}
        }
    }

    /** Every species in [biome] has now been caught by someone this run. */
    fun onBiomeComplete(biome: SafariBiome) {
        if (!config.biomeDone) return
        if (onCooldown("biome:${biome.name}")) return
        announce("${biome.displayName} Done!", 1.4f)
    }

    /** Everything but the Macaw is caught, usually the real finish line for a run. */
    fun onAllButMacaw() {
        if (!config.allButMacaw) return
        announce("Everything except Macaw done!", 1.6f)
    }

    /** All 37 caught by someone. */
    fun onAllDone() {
        if (!config.allDone) return
        announce("Everything Done!", 2.0f)
    }

    /**
     * A Macaw has turned up.
     *
     * Not a staged encounter like the bosses, so it says the one thing there is to say. [where] is the spot to send
     * people to, or null when the Birdfeeder announced it from out of range.
     */
    fun onMacawSpawn(where: String?) {
        if (!config.macaw) return
        val detail = where?.let { " ($it)" }.orEmpty()
        TitleManager.sendTitle("§6§lMACAW!", where ?: "somewhere in the Forest")
        sound(1.6f)
        ChatUtils.chat("§6Macaw spawned!$detail")
        post(partyConfig.macaw, "Macaw spawned!$detail")
    }

    private fun announce(text: String, pitch: Float) {
        TitleManager.sendTitle("§a§l$text")
        sound(pitch)
        ChatUtils.chat("§a$text")
        post(partyConfig.milestones, text)
    }

    private fun fire(critter: SafariCritter, stage: Stage, detail: String, pitch: Float) {
        if (!alertsOn(critter)) return
        if (!inItsBiome(critter)) return
        // Gemzie chambers repeat every few minutes and its ready and done pair can be seconds apart, so the
        // anti-repeat cooldown must not apply to it.
        if (critter != SafariCritter.GEMZIE && onCooldown("${critter.name}:$stage")) return

        val name = critter.displayName.uppercase()
        TitleManager.sendTitle("${stageColor(stage)}§l$name ${stage.name}", detail)
        sound(pitch)
        ChatUtils.chat("§e$name ${stage.name} §7- $detail")
        post(broadcastFor(critter), "${critter.displayName} ${stage.name.lowercase()}")
    }

    private fun stageColor(stage: Stage) = when (stage) {
        Stage.READY -> "§6"
        Stage.STARTED -> "§c"
        Stage.DONE -> "§a"
    }

    /**
     * Whether the encounter is happening where the player is.
     *
     * Each of the three belongs to one biome, and in a party split a biome each only one person is standing in it.
     * Fails open: when the biome cannot be worked out the alert fires, since silence on a "cannot tell" would look
     * exactly like the feature being broken.
     */
    private fun inItsBiome(critter: SafariCritter): Boolean {
        if (testing || !config.encountersInBiomeOnly) return true
        val here = SafariAreaApi.currentBiome ?: return true
        return critter.biome == here
    }

    private fun alertsOn(critter: SafariCritter) = when (critter) {
        SafariCritter.GEMZIE -> config.gemzie
        SafariCritter.WUMPA -> config.wumpa
        SafariCritter.DOOMSPIRAL -> config.doomspiral
        else -> false
    }

    private fun broadcastFor(critter: SafariCritter) = when (critter) {
        SafariCritter.GEMZIE -> partyConfig.gemzie
        SafariCritter.WUMPA -> partyConfig.wumpa
        SafariCritter.DOOMSPIRAL -> partyConfig.doomspiral
        else -> SafariBroadcast.NONE
    }

    /** Sends one line to whoever the setting names, or nowhere. */
    fun post(to: SafariBroadcast, message: String) {
        when (to) {
            SafariBroadcast.NONE -> return
            SafariBroadcast.PARTY -> if (PartyApi.isInParty()) HypixelCommands.partyChat(message)
            SafariBroadcast.ALL -> HypixelCommands.allChat(message)
        }
    }

    /** True if this stage already fired recently, so it should be suppressed. */
    private fun onCooldown(key: String): Boolean {
        val previous = lastFired[key]
        if (previous != null && previous.passedSince() < stageCooldown) return true
        lastFired[key] = SimpleTimeMark.now()
        return false
    }

    /** Silent unless asked for: a run fires plenty of these. */
    private fun sound(pitch: Float) {
        if (!config.alertSound) return
        SoundUtils.createSound("block.note_block.pling", pitch).playSound()
    }

    /** Fires every alert now, with the biome gate lifted, so they can be checked without waiting. */
    fun test() {
        testing = true
        try {
            for (critter in listOf(SafariCritter.GEMZIE, SafariCritter.WUMPA, SafariCritter.DOOMSPIRAL)) {
                for (stage in Stage.entries) {
                    lastFired.clear()
                    fire(critter, stage, "test", 1.4f)
                }
            }
            onMacawSpawn("Forest 0 70 0")
        } finally {
            testing = false
        }
    }

    /** Clears per stage cooldowns; called when a new run starts. */
    fun reset() {
        lastFired.clear()
        gemzieRemaining = 0
    }
}
