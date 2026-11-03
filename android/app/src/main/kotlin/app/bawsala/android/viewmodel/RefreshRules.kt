package app.bawsala.android.viewmodel

import app.bawsala.android.sync.SyncResult

/** What pulling down to refresh says when it is done (#67). */
object RefreshRules {
    fun messageKey(result: SyncResult): String =
        when (result) {
            SyncResult.DONE -> "upToDate"
            // Signed out: the screen was recomputed on the device; nothing to send.
            SyncResult.NOT_SIGNED_IN -> "refreshedOnDevice"
            SyncResult.RETRY -> "syncOffline"
            SyncResult.FAILED -> "syncFailed"
        }
}
