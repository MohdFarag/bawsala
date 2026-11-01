package app.bawsala.android

import android.content.Context
import app.bawsala.android.data.Repository
import app.bawsala.android.domain.DayPlanner
import app.bawsala.android.domain.RuleScheduler
import app.bawsala.android.media.MediaFiles
import app.bawsala.android.sync.AccountService
import app.bawsala.android.sync.AccountStore
import app.bawsala.android.sync.ApiClient
import app.bawsala.android.sync.SyncRunner
import app.bawsala.android.work.SyncWorker
import java.io.File

/**
 * Creates the app's long-lived objects once; the UI and background workers receive them from here.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val repository = Repository(context)
    val dayPlanner = DayPlanner()
    val ruleScheduler = RuleScheduler(repository, dayPlanner)
    val mediaDir: File = MediaFiles.dir(context)
    val accounts = AccountStore(context)
    private val api = ApiClient(accounts)
    val syncRunner = SyncRunner(accounts, api, repository, mediaDir)
    val accountService = AccountService(accounts, api, repository)

    init {
        // Every local change is sent soon after it is saved, while signed in.
        repository.onLocalChange = {
            if (accounts.account.value != null) SyncWorker.requestSoon(appContext)
        }
    }
}
