package app.bawsala.android.navigation

/** Every destination in the app. Tab roots first, then screens opened on top of them. */
sealed interface Screen {
    data object Today : Screen

    data object Inbox : Screen

    data object Plan : Screen

    data object More : Screen

    /**
     * V3 "المشاريع" tab. Until the PARA projects screen (phase 4) it links to tasks and
     * collections.
     */
    data object Projects : Screen

    data class Process(val itemId: String? = null) : Screen

    data class TaskDetail(val id: String, val fresh: Boolean = false) : Screen

    data object Focus : Screen

    data object Search : Screen

    data object Tasks : Screen

    data object Collections : Screen

    data object Trash : Screen

    data object PrayerSettings : Screen

    data object Blocks : Screen

    data object Lists : Screen

    data object Preferences : Screen

    data object Account : Screen

    data object Calendars : Screen

    /** Every V3 design token and component (debug builds only). */
    data object DesignGallery : Screen
}

/**
 * The V3 tabs, in reading order (right to left in Arabic): Today and Triage before the capture
 * button, Projects and Review after it. Triage opens the inbox and Review the planner until their
 * V3 screens arrive (phases 4 and 5).
 */
enum class Tab(val root: Screen) {
    TODAY(Screen.Today),
    TRIAGE(Screen.Inbox),
    PROJECTS(Screen.Projects),
    REVIEW(Screen.Plan),
}

/** Tab roots and the secondary screens that keep the bottom navigation visible. */
val Screen.isTabRoot: Boolean
    get() = Tab.entries.any { it.root == this }
val Screen.showsBottomBar: Boolean
    get() =
        isTabRoot ||
            this in
                setOf(Screen.Search, Screen.Tasks, Screen.Collections, Screen.Trash, Screen.More)
