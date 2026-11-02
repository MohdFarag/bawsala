package app.bawsala.android.navigation

/** Every destination in the app. Tab roots first, then screens opened on top of them. */
sealed interface Screen {
    data object Today : Screen

    data object Inbox : Screen

    data object Plan : Screen

    /** V3 "المراجعة" tab: the weekly review; the planner opens from it. */
    data object Review : Screen

    data object More : Screen

    /** V3 "المشاريع" tab: projects, areas, resources and archive (PARA). */
    data object Projects : Screen

    /** One idea on the Triage card, opened from elsewhere (search, a task's source). */
    data class Process(val itemId: String? = null) : Screen

    /** Every idea waiting, as a list with filters ("All ideas" from Triage). */
    data object InboxList : Screen

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
 * button, Projects and Review after it. Review opens the planner until its V3 screen arrives (phase
 * 5).
 */
enum class Tab(val root: Screen) {
    TODAY(Screen.Today),
    TRIAGE(Screen.Inbox),
    PROJECTS(Screen.Projects),
    REVIEW(Screen.Review),
}

/** Tab roots and the secondary screens that keep the bottom navigation visible. */
val Screen.isTabRoot: Boolean
    get() = Tab.entries.any { it.root == this }
val Screen.showsBottomBar: Boolean
    get() =
        isTabRoot ||
            this in
                setOf(
                    Screen.Search,
                    Screen.Tasks,
                    Screen.Collections,
                    Screen.Trash,
                    Screen.More,
                    Screen.InboxList,
                    Screen.Plan,
                )
