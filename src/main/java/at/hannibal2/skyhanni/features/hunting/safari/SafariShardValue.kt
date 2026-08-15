package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.hunting.safari.SafariProfitConfig.SafariPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat

/**
 * What a run's shards are worth, at bazaar prices.
 *
 * Every species' shard is a bazaar product, so a run's value is simply the shards it gave priced one species at a
 * time. Catch counts cannot stand in for that: a catch gives a random number of shards, and the shards themselves
 * are worth wildly different amounts.
 *
 * Both sides of the spread are available, and they are a long way apart. **Instant sell** is what selling to the
 * bazaar pays this second; **sell offer** is what you would list at and wait for.
 */
object SafariShardValue {

    private val config get() = SkyHanniMod.feature.hunting.safari.profit

    private fun SafariPriceSource.toItemPriceSource() = when (this) {
        SafariPriceSource.INSTANT_SELL -> ItemPriceSource.BAZAAR_INSTANT_SELL
        SafariPriceSource.SELL_OFFER -> ItemPriceSource.BAZAAR_INSTANT_BUY
    }

    /** Bazaar tax on the sell side, which both price modes are. */
    private const val BAZAAR_TAX = 0.0125

    /** What one [critter] shard fetches on [source]'s side, tax taken off if the settings say so. */
    fun price(critter: SafariCritter, source: SafariPriceSource = config.priceSource): Double {
        val raw = critter.shardInternalName.getPrice(source.toItemPriceSource())
        return if (config.subtractTax) raw * (1 - BAZAAR_TAX) else raw
    }

    /** What a pile of shards is worth. Species with no price simply add nothing. */
    fun valueOf(shards: Map<SafariCritter, Int>, source: SafariPriceSource = config.priceSource): Long =
        shards.entries.sumOf { (critter, amount) -> price(critter, source) * amount }.toLong()

    fun valueOf(session: SafariRunSession?, source: SafariPriceSource = config.priceSource): Long =
        session?.let { valueOf(it.shards, source) } ?: 0L

    /** What a saved run's shards are worth, or 0 if it kept no breakdown. */
    fun valueOf(run: SafariRunRecord, source: SafariPriceSource = config.priceSource): Long {
        if (!run.hasShardData()) return 0
        // A saved run holds species by name, so one dropped from the roster since is simply skipped.
        return run.shards.entries.sumOf { (name, amount) ->
            SafariCritter.byName(name)?.let { price(it, source) * amount } ?: 0.0
        }.toLong()
    }

    /** Every priceable run added up. Runs with no breakdown are left out, not zeroed. */
    fun totalValue(runs: List<SafariRunRecord>): Long = runs.sumOf { valueOf(it) }

    /**
     * A run's value as it is written out, in the one place that decides the wording.
     *
     * One figure normally. With **show both prices** on, the other side follows it, named, because the two are far
     * apart and the gap is the point.
     */
    fun coinsText(session: SafariRunSession?): String {
        val chosen = config.priceSource
        val text = "${valueOf(session, chosen).shortFormat()} coins"
        if (!config.showBothPrices) return text

        val other = if (chosen == SafariPriceSource.SELL_OFFER) SafariPriceSource.INSTANT_SELL
        else SafariPriceSource.SELL_OFFER
        return "$text §7· §6${valueOf(session, other).shortFormat()} ${name(other)}"
    }

    /** How a price side is referred to in a sentence. */
    fun name(source: SafariPriceSource) =
        if (source == SafariPriceSource.SELL_OFFER) "as sell offers" else "instant sell"

    fun formatCoins(coins: Long): String = if (coins >= 10_000) coins.shortFormat() else coins.addSeparators()
}
