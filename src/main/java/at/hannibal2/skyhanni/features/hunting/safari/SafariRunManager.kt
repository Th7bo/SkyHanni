package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark

/**
 * Owns the live run: starts one on arriving at the Critter Safari, feeds it parsed chat events, and closes it on
 * the way out.
 *
 * A run tracks what this party has caught **since walking in**, which the in game Critterdex cannot, because that
 * counts what you have caught ever.
 */
@SkyHanniModule
object SafariRunManager {

    var currentSession: SafariRunSession? = null
        private set

    /** The last finished run, so the displays and commands still work after leaving. */
    var lastSession: SafariRunSession? = null
        private set

    private val announcedBiomes = mutableSetOf<SafariBiome>()
    private var announcedAllButMacaw = false
    private var announcedAllDone = false

    /** The run in progress if there is one, otherwise the most recent finished run. */
    val currentOrLast: SafariRunSession? get() = currentSession ?: lastSession

    @HandleEvent
    private fun onIslandJoin(event: IslandJoinEvent) {
        if (event.island != IslandType.SAFARI) return
        startSession()
    }

    @HandleEvent
    private fun onIslandLeave(event: IslandLeaveEvent) {
        if (event.island != IslandType.SAFARI) return
        endSession()
    }

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val catchEvent = SafariChatParser.parse(event.cleanMessage) ?: return
        // A catch arriving before the island change has landed is proof of presence in its own right.
        val session = currentSession ?: startSession()

        session.record(catchEvent)
        SafariRecatchHelper.onCatchEvent(catchEvent)
        if (!catchEvent.type.isCatch) return

        SafariEncounterAlerts.onCatch(catchEvent.critter)
        announceNewlyCompleteBiomes(session)
        announceRunMilestones(session)
    }

    /**
     * Fires a completion alert the moment a biome's last species is caught by anyone. Each biome announces at most
     * once per run.
     */
    private fun announceNewlyCompleteBiomes(session: SafariRunSession) {
        for (biome in SafariBiome.entries) {
            if (biome in announcedBiomes) continue
            if (!session.biomeComplete(biome)) continue
            announcedBiomes.add(biome)
            SafariEncounterAlerts.onBiomeComplete(biome)
        }
    }

    /**
     * The two whole run milestones, at most once each per run.
     *
     * They are mutually exclusive: if a single catch completes the dex outright, only "Everything done" fires, and
     * the weaker "except Macaw" message is marked as announced so it cannot follow it.
     */
    private fun announceRunMilestones(session: SafariRunSession) {
        if (!announcedAllDone && session.dexComplete()) {
            announcedAllDone = true
            announcedAllButMacaw = true
            SafariEncounterAlerts.onAllDone()
            return
        }
        if (!announcedAllButMacaw && session.allCaughtExcept(SafariCritter.MACAW)) {
            announcedAllButMacaw = true
            SafariEncounterAlerts.onAllButMacaw()
        }
    }

    /**
     * Opens a run, filing whatever was open before it.
     *
     * Opened on arrival rather than at the first catch, so the display reads 0/37 on the way in instead of showing
     * the run before it.
     */
    fun startSession(): SafariRunSession {
        endSession()
        val session = SafariRunSession(PlayerUtils.getName(), SimpleTimeMark.now())
        currentSession = session

        announcedBiomes.clear()
        announcedAllButMacaw = false
        announcedAllDone = false
        SafariEncounterAlerts.reset()
        SafariTraderTracker.reset()
        SafariNestTracker.reset()
        SafariMoundTracker.reset()
        SafariRecatchHelper.reset()
        SafariBirdfeeder.reset()
        SafariHotspot.reset()
        SafariFloorDrops.reset()
        SafariHideyhoSolver.reset()
        return session
    }

    private fun endSession() {
        val finished = currentSession ?: return
        currentSession = null
        if (finished.isEmpty()) return
        lastSession = finished
        SafariRunHistory.record(finished)
    }

    /** Wipes the active run's tallies without waiting to leave the island. */
    fun reset() {
        currentSession = null
        startSession()
    }

    /**
     * Works out what the run can no longer produce.
     *
     * Snoozle comes from the breakable Cavern walls. Once all of them are confirmed broken and none has turned up,
     * there is no way for one to appear, so it stops being listed as outstanding.
     */
    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onSecondPassed() {
        val session = currentSession ?: return
        val snoozle = SafariCritter.SNOOZLE
        val gone = SafariWallTracker.SNOOPER.allConfirmedBroken() &&
            session.partyCatches(snoozle) == 0 &&
            session.nearby(snoozle) == 0
        session.unavailable = if (gone) setOf(snoozle) else emptySet()
    }
}
