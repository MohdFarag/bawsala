package app.bawsala.android.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The navigation step decides which way screens move (#64). */
class NavigatorMoveTest {
    @Test
    fun `opening goes forward, back goes back, tabs and replacements cross-fade`() {
        val nav = Navigator()
        assertEquals(NavMove.SWITCH, nav.lastMove)
        nav.open(Screen.Search)
        assertEquals(NavMove.FORWARD, nav.lastMove)
        nav.replace(Screen.More)
        assertEquals(NavMove.SWITCH, nav.lastMove)
        nav.back()
        assertEquals(NavMove.BACK, nav.lastMove)
        nav.switchTo(Tab.PROJECTS)
        assertEquals(NavMove.SWITCH, nav.lastMove)
        // At a tab root there is nothing to go back to, and the last move stays as it was.
        assertFalse(nav.back())
        assertEquals(NavMove.SWITCH, nav.lastMove)
    }
}
