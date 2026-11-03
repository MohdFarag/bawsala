package app.bawsala.android.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The last navigation step, so screen transitions can move the right way (#64). */
enum class NavMove {
    /** A screen opened on top: it comes in from the end side. */
    FORWARD,
    /** Back: the current screen leaves toward the end side. */
    BACK,
    /** A tab switch or a replacement: a cross-fade. */
    SWITCH,
}

/** A back stack per tab: switching tabs starts a fresh stack; Back pops within it. */
class Navigator {
    private val stack = mutableStateListOf<Screen>(Screen.Today)

    var lastMove by mutableStateOf(NavMove.SWITCH)
        private set

    val current: Screen
        get() = stack.last()

    /** The screen under the current one, e.g. what stays visible behind the search sheet. */
    val beneath: Screen?
        get() = stack.getOrNull(stack.lastIndex - 1)

    val currentTab: Tab
        get() = Tab.entries.firstOrNull { it.root == stack.first() } ?: Tab.TODAY

    fun open(screen: Screen) {
        lastMove = NavMove.FORWARD
        stack += screen
    }

    fun replace(screen: Screen) {
        lastMove = NavMove.SWITCH
        stack[stack.lastIndex] = screen
    }

    /** Returns false when already at a tab root, so the caller can leave the app. */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        lastMove = NavMove.BACK
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun switchTo(tab: Tab) {
        lastMove = NavMove.SWITCH
        stack.clear()
        stack += tab.root
    }
}
