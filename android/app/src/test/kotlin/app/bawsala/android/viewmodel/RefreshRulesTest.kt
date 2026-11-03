package app.bawsala.android.viewmodel

import app.bawsala.android.sync.SyncResult
import kotlin.test.Test
import kotlin.test.assertEquals

class RefreshRulesTest {
    @Test
    fun `each sync result has its own message`() {
        assertEquals("upToDate", RefreshRules.messageKey(SyncResult.DONE))
        assertEquals("refreshedOnDevice", RefreshRules.messageKey(SyncResult.NOT_SIGNED_IN))
        assertEquals("syncOffline", RefreshRules.messageKey(SyncResult.RETRY))
        assertEquals("syncFailed", RefreshRules.messageKey(SyncResult.FAILED))
    }
}
