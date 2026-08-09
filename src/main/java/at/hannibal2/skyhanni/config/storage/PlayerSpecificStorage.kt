package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.chat.CurrentChatDisplay
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.fame.UpgradeReminder.CommunityShopUpgrade
import at.hannibal2.skyhanni.features.misc.UserLuckBreakdown
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import com.google.gson.annotations.Expose
import java.time.LocalDate
import kotlin.time.Duration

class PlayerSpecificStorage {
    @Expose
    var profiles: MutableMap<String, ProfileSpecificStorage> = mutableMapOf() // profile name

    @Expose
    var multipleProfiles: Boolean = false

    @Expose
    var useRomanNumerals: Boolean = true

    @Expose
    var gardenCommunityUpgrade: Int = -1

    @Expose
    var fameRank: String = "New Player"

    @Expose
    var currentChat: CurrentChatDisplay.ChatType? = null

    @Expose
    var nextCityProjectParticipationTime: SimpleTimeMark = farPast()

    @Expose
    var communityShopAccountUpgrade: CommunityShopUpgrade? = null

    @Expose
    var guildMembers: MutableList<String> = mutableListOf()

    @Expose
    var bazaar: BazaarStorage = BazaarStorage()

    class BazaarStorage {
        @Expose
        var taxRate: Double = 1.25

        @Expose
        var coinsTowardsLimit: Double = 0.0

        @Expose
        var lastAccessedDay: LocalDate? = null
    }

    @Expose
    var winter: WinterStorage = WinterStorage()

    class WinterStorage {
        @Expose
        var playersThatHaveBeenGifted: MutableSet<String> = mutableSetOf()

        @Expose
        var amountGifted: Int = 0

        @Expose
        var cakeCollectedYear: Int = 0
    }

    @Expose
    var bingoSessions: MutableMap<Long, BingoSession> = mutableMapOf()

    class BingoSession {
        @Expose
        var tierOneMinionsDone: MutableSet<NeuInternalName> = mutableSetOf()

        @Expose
        var goals: MutableMap<Int, BingoGoal> = mutableMapOf()
    }

    @Expose
    var limbo: LimboStats = LimboStats()

    @Expose
    var skyblockDailyPlaytime: DailySkyblockPlaytimeStorage = DailySkyblockPlaytimeStorage()

    class DailySkyblockPlaytimeStorage {
        /** Keys: ISO-local `yyyy-MM-dd`. Values: tracked seconds while in SkyBlock that day. */
        @Expose
        var secondsByIsoDate: MutableMap<String, Long> = mutableMapOf()

        /**
         * Per-island breakdown of [secondsByIsoDate].
         * Outer keys: ISO-local `yyyy-MM-dd`. Inner keys: [at.hannibal2.skyhanni.data.IslandType] enum names.
         *
         * Only filled since island tracking was added, so days recorded by older versions have no entry here
         * (and days may have a smaller island sum than their [secondsByIsoDate] total).
         */
        @Expose
        var islandSecondsByIsoDate: MutableMap<String, MutableMap<String, Long>> = mutableMapOf()

        /** Persisted all-time best single-day seconds. Survives history pruning. 0 = uninitialized (migrated on first use). */
        @Expose
        var allTimeMaxSeconds: Long = 0L

        /** ISO-local date of the all-time best day, for display only. May no longer be in [secondsByIsoDate]. */
        @Expose
        var allTimeMaxDate: String? = null
    }

    class LimboStats {
        @Expose
        var playtime: Int = 0

        @Expose
        var personalBest: Int = 0

        /**
         * Do NOT use if you are trying to get the players total user luck
         *
         * @see UserLuckBreakdown.getTotalUserLuck
         */
        @Expose
        var userLuck: Float = 0f
    }

    @Expose
    var slayerPersonalBests: MutableMap<BossType, Duration> = mutableMapOf()

    @Expose
    var easterEggs: EasterEggStorage = EasterEggStorage()

    class EasterEggStorage {
        @Expose
        var seen: MutableSet<String> = mutableSetOf()

        @Expose
        var disabled: MutableSet<String> = mutableSetOf()
    }
}
