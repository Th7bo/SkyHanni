package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.chestWrapperPattern
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.genericMiningRewardMessage
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.lockPickedPattern
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.lootChestCollectedPattern
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.rewardHeaderPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils

/**
 * Collapses the multi line chest reward block from powder mining into a single line, with the original block
 * kept on hover.
 *
 * Only lines that are recognized as part of a chest block are blocked. The one exception are the two `▬` border
 * lines, which have to be decided on before the header line that identifies the block has arrived. A `§e` or `§d`
 * border block on a mining island that is not a chest therefore loses its borders but keeps all of its content.
 */
@SkyHanniModule
object PowderMiningCompactChat {

    private val config get() = SkyHanniMod.feature.chat.filterType.powderMining

    private const val BLOCK_REASON = "powder_mining_compact"

    private var insideBlock = false

    /** The header line of the current block, which is also what identifies it as a chest block. */
    private var header: String? = null
    private val originalLines = mutableListOf<String>()
    private val rewards = mutableListOf<String>()

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.MINING])
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.compact) return
        // The reward colors are the point of the compacted line, so cleanMessage is not an option here.
        @Suppress("DEPRECATION")
        val message = event.message

        if (chestWrapperPattern.matches(message)) {
            if (insideBlock) finishBlock() else startBlock()
            event.blockedReason = BLOCK_REASON
            return
        }
        if (!insideBlock) return

        if (collect(message)) event.blockedReason = BLOCK_REASON
    }

    private fun startBlock() {
        insideBlock = true
        header = null
        originalLines.clear()
        rewards.clear()
    }

    /** @return whether the line was consumed by the compacted message, and should be hidden. */
    private fun collect(message: String): Boolean {
        if (lockPickedPattern.matches(message) || lootChestCollectedPattern.matches(message)) {
            header = message.trim()
            originalLines.add(message)
            return true
        }

        // Until the header identifies this as a chest block, nothing else is touched.
        if (header == null) return false

        if (StringUtils.isEmpty(message) || rewardHeaderPattern.matches(message)) return true

        return genericMiningRewardMessage.matchMatcher(message) {
            val reward = groupOrEmpty("reward")
            val amount = groupOrNull("amount")
            rewards.add(if (amount != null) "$reward §8x$amount" else reward)
            originalLines.add(message)
            true
        } ?: false
    }

    private fun finishBlock() {
        insideBlock = false
        val header = header
        if (header != null && rewards.isNotEmpty()) {
            ChatUtils.hoverableChat(
                "$header §8» ${rewards.joinToString("§7, ")}",
                hover = originalLines,
                prefix = false,
            )
        }
        this.header = null
        originalLines.clear()
        rewards.clear()
    }

    @HandleEvent
    private fun onWorldChange() {
        insideBlock = false
        header = null
        originalLines.clear()
        rewards.clear()
    }
}
