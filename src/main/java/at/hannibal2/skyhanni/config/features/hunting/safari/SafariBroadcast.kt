package at.hannibal2.skyhanni.config.features.hunting.safari

/** Where one of the Safari announcements goes. Every announcement chooses its own audience. */
enum class SafariBroadcast(private val displayName: String) {
    /** Kept to yourself. */
    NONE("Nobody"),

    /** `/pc` - the party. */
    PARTY("Party chat"),

    /** `/ac` - everyone on the island. */
    ALL("All chat"),
    ;

    override fun toString() = displayName
}
