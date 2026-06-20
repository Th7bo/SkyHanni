package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.toLorenzVec
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.milliseconds

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@SkyHanniModule
object CorpseLocator {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseLocator

    /**
     * REGEX-TEST: x: -164, y: 8, z: -154 | (Lapis Corpse)
     * REGEX-TEST: x: 141, y: 14, z: -131
     * REGEX-TEST: x: -9, y: 135, z: 20 | (Tungsten Corpse)
     */
    private val mineshaftCoordsPattern by RepoPattern.pattern(
        "mineshaft.corpse.coords",
        "x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)(?:.+)?",
    )

    private val sharedWaypoints: MutableList<LorenzVec> = mutableListOf()

    // Corpses detected and scheduled to be marked after a random delay, but not yet added as a waypoint.
    private val pendingCorpses: MutableList<LorenzVec> = mutableListOf()

    // TODO: use entity events
    @OptIn(AllEntitiesGetter::class)
    private fun findCorpse() {
        EntityUtils.getAllEntities().filterIsInstance<ArmorStand>()
            .filterNot { corpse -> MineshaftWaypoints.waypoints.any { it.location.distance(corpse.getLorenzVec()) <= 3 } }
            .filterNot { corpse -> pendingCorpses.any { it.distance(corpse.getLorenzVec()) <= 3 } }
            .filter { entity ->
                entity.showArms() && entity.showBasePlate().not() && !entity.isInvisible
            }
            .forEach { entity ->
                val helmetName = entity.getStandHelmet()?.getInternalName() ?: return@forEach
                val corpseType = MineshaftWaypointType.getByHelmetOrNull(helmetName) ?: return@forEach

                val location = entity.getLorenzVec()
                pendingCorpses.add(location)

                DelayedRun.runDelayed((500..3000).random().milliseconds) {
                    pendingCorpses.remove(location)
                    // Skip if a waypoint was already added near here while we were waiting.
                    if (MineshaftWaypoints.waypoints.any { it.location.distance(location) <= 3 }) return@runDelayed

                    val article = if (corpseType.displayText == "Umber Corpse") "an" else "a"
                    ChatUtils.chat("Located $article ${corpseType.displayText} and marked its location with a waypoint.")

                    MineshaftWaypoints.waypoints.add(
                        MineshaftWaypoint(
                            waypointType = corpseType,
                            location = location.up(),
                            isCorpse = true,
                        ),
                    )
                }
            }
    }

    private fun shareCorpse() {
        val closestCorpse = MineshaftWaypoints.waypoints.filter { it.isCorpse && !it.shared }
            .filterNot { corpse ->
                sharedWaypoints.any { corpse.location.distance(it) <= 5 }
            }
            .filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        val location = closestCorpse.location.toChatFormat()
        val type = closestCorpse.waypointType.displayText

        HypixelCommands.partyChat("$location | ($type)")
        closestCorpse.shared = true
    }


    @HandleEvent
    fun onWorldChange() {
        sharedWaypoints.clear()
        pendingCorpses.clear()
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        findCorpse()

        if (!config.autoSendLocation) return
        if (MineshaftWaypoints.waypoints.isEmpty()) return
        if (PartyApi.partyMembers.isEmpty()) return
        shareCorpse()
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    @HandleEvent
    fun onAllChat(event: PlayerAllChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    private fun handleChatEvent(author: String, message: String) {
        if (!isEnabled()) return
        if (PlayerUtils.getName() in author) return

        mineshaftCoordsPattern.matchMatcher(message) {
            val location = toLorenzVec() ?: return

            // Return if someone had already sent a location nearby
            if (sharedWaypoints.any { it.distance(location) <= 5 }) return
            sharedWaypoints.add(location)
        }
    }

    fun isEnabled() = IslandType.MINESHAFT.isInIsland() && config.enabled
}
