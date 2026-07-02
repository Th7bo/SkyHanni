package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose

enum class RaffleTaskDifficulty(val displayName: String, val color: String) {
    EASY("Easy", "§a"),
    MEDIUM("Medium", "§e"),
    HARD("Hard", "§c"),
}

data class RaffleTask(
    @Expose val difficulty: RaffleTaskDifficulty = RaffleTaskDifficulty.EASY,
    @Expose val name: String = "",
    @Expose val objective: String = "",
    @Expose val completed: Boolean = false,
)

@SkyHanniModule
object RaffleTaskTracker {

    private val config get() = SkyHanniMod.feature.event.anniversaryCelebration400
    private val storage get() = ProfileStorageData.profileSpecific?.raffleTasks

    private const val OBJECTIVE_WRAP_WIDTH = 180

    private val patternGroup = RepoPattern.group("event.century.raffletasks")

    /**
     * REGEX-TEST: Raffle Tasks
     */
    private val taskInventoryPattern by patternGroup.pattern(
        "inventory.tasks",
        "Raffle Tasks",
    )

    /**
     * REGEX-TEST: Year 500 Incredible Raffle Box
     */
    private val raffleBoxInventoryPattern by patternGroup.pattern(
        "inventory.box",
        "Year \\d+ .*Raffle Box?",
    )

    /**
     * REGEX-TEST: Easy Task
     * REGEX-TEST: Medium Task
     * REGEX-TEST: Hard Task
     */
    private val difficultyPattern by patternGroup.pattern(
        "difficulty",
        "(?<difficulty>Easy|Medium|Hard) Task",
    )

    /**
     * REGEX-TEST: Time until reset: 1h 32m 51s
     */
    private val resetTimePattern by patternGroup.pattern(
        "reset",
        "Time until reset: (?<time>.+)",
    )

    /**
     * REGEX-TEST: RAFFLE TASK! You completed the Lily Pad Exploder raffle task and earned +1 Raffle Ticket and a slice of cake!
     */
    private val taskCompletePattern by patternGroup.pattern(
        "chat.complete",
        "RAFFLE TASK! You completed the (?<name>.+?) raffle task and earned .*",
    )

    private var display: List<Renderable> = emptyList()
    private var dirty = true

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.showRaffleTasks) return
        if (!SkyBlockUtils.inSkyBlock) return
        val name = event.inventoryName
        if (taskInventoryPattern.matches(name)) {
            readTasks(event.inventoryItems)
            readResetTime(event.inventoryItems)
        } else if (raffleBoxInventoryPattern.matches(name)) {
            readResetTime(event.inventoryItems)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.showRaffleTasks) return
        val store = storage ?: return
        taskCompletePattern.matchMatcher(event.cleanMessage) {
            val name = group("name").trim()
            val index = store.tasks.indexOfFirst { !it.completed && it.name.removeColor().trim() == name }
            if (index == -1) return
            store.tasks[index] = store.tasks[index].copy(completed = true)
            dirty = true
        }
    }

    private fun readTasks(items: Map<Int, SafeItemStack>) {
        val store = storage ?: return
        val parsed = mutableListOf<RaffleTask>()
        for (stack in items.values) {
            val lore = stack.getLore()
            val clean = lore.map { it.removeColor() }

            val difficulty = clean.firstNotNullOfOrNull { line ->
                difficultyPattern.matchMatcher(line) {
                    when (group("difficulty")) {
                        "Easy" -> RaffleTaskDifficulty.EASY
                        "Medium" -> RaffleTaskDifficulty.MEDIUM
                        "Hard" -> RaffleTaskDifficulty.HARD
                        else -> null
                    }
                }
            } ?: continue

            val difficultyIndex = clean.indexOfFirst { difficultyPattern.matches(it) }
            val statusIndex = clean.indexOfFirst { it == "COMPLETE" || it == "INCOMPLETE" }
            if (statusIndex == -1) continue
            val completed = clean[statusIndex] == "COMPLETE"

            val objective = lore.subList((difficultyIndex + 1).coerceAtMost(statusIndex), statusIndex)
                .firstOrNull { it.removeColor().isNotBlank() }
                ?.trim()
                ?: stack.hoverName.string

            parsed.add(RaffleTask(difficulty, stack.hoverName.string, objective, completed))
        }
        if (parsed.isEmpty()) return
        store.tasks = parsed
        dirty = true
    }

    private fun readResetTime(items: Map<Int, SafeItemStack>) {
        val store = storage ?: return
        for (stack in items.values) {
            val time = resetTimePattern.firstMatcher(stack.getLore().map { it.removeColor() }) {
                TimeUtils.getDurationOrNull(group("time"))
            } ?: continue
            store.resetTime = SimpleTimeMark.now() + time
            dirty = true
            return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (isEnabled()) dirty = true
    }

    init {
        // Rendering through RenderDisplayHelper (instead of a plain overlay event) so the filter
        // button stays clickable while the player's own inventory is open, like the trackers.
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() },
            onRender = {
                if (dirty) {
                    display = buildDisplay()
                    dirty = false
                }
                config.raffleTasksPosition.renderRenderables(display, posLabel = "Raffle Tasks")
            },
        )
    }

    private fun buildDisplay(): List<Renderable> = buildList {
        val store = storage ?: return@buildList

        val resetString = when {
            store.resetTime == SimpleTimeMark.farPast() -> ""
            store.resetTime.isInPast() -> " §8- §cResetting..."
            else -> " §8- §7resets in §e${store.resetTime.timeUntil().format()}"
        }
        add(Renderable.text("§e§lRaffle Tasks$resetString"))

        addFilterToggle()

        val tasks = store.tasks
        val filter = store.selectedFilter
        val difficulties = filter.difficulty?.let { listOf(it) } ?: RaffleTaskDifficulty.entries

        for (difficulty in difficulties) {
            val difficultyTasks = tasks.filter { it.difficulty == difficulty }
            if (difficultyTasks.isEmpty()) continue
            val remaining = difficultyTasks.filter { !it.completed }

            add(Renderable.text("${difficulty.color}§l${difficulty.displayName} §r§7(${remaining.size}/${difficultyTasks.size} left)"))
            if (remaining.isEmpty()) {
                add(Renderable.text("  §a§oAll complete!"))
            } else {
                remaining.forEach { add(Renderable.wrappedText("  §7- §f${it.objective}", setWidth = OBJECTIVE_WRAP_WIDTH)) }
            }
        }
    }

    private fun MutableList<Renderable>.addFilterToggle() {
        val store = storage ?: return
        addRenderableButton<RaffleFilter>(
            label = "Filter",
            current = store.selectedFilter,
            onChange = {
                store.selectedFilter = it
                dirty = true
            },
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.showRaffleTasks && !storage?.tasks.isNullOrEmpty()

    enum class RaffleFilter(private val displayName: String, val difficulty: RaffleTaskDifficulty?) {
        ALL("All", null),
        EASY("Easy", RaffleTaskDifficulty.EASY),
        MEDIUM("Medium", RaffleTaskDifficulty.MEDIUM),
        HARD("Hard", RaffleTaskDifficulty.HARD),
        ;

        override fun toString() = displayName
    }
}
