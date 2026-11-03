package app.bawsala.android.device

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.bawsala.android.AppContainer
import app.bawsala.android.domain.CaptureFactory
import app.bawsala.android.domain.CaptureInput
import app.bawsala.android.sync.SyncResult
import app.bawsala.android.sync.Tokens
import app.bawsala.core.model.CaptureKind
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Two real SQLite/Keystore-backed installations with distinct accounts, outboxes and cursors. */
@RunWith(AndroidJUnit4::class)
class SyncSmokeTest {
    @Test fun signUpSignInAndMergeBetweenIsolatedInstallations() = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("testServer") ?: error("testServer is required")
        require(URI(url).host in setOf("10.0.2.2", "127.0.0.1", "localhost")) { "Never test against production" }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val a = AppContainer(Installation(context, "device-a"), backgroundWork = false)
        val b = AppContainer(Installation(context, "device-b"), backgroundWork = false)
        a.repository.load()
        b.repository.load()
        val email = "device-${UUID.randomUUID()}@example.com"
        val password = "device test password"
        a.accountService.signIn(url, email, password, true, "en")
        b.accountService.signIn(url, email, password, false, "en")
        assertEquals(a.accounts.account.value?.userId, b.accounts.account.value?.userId)
        assertNotEquals(a.accounts.deviceId, b.accounts.deviceId)
        val item = CaptureFactory.create("shared-capture", CaptureInput(CaptureKind.TEXT, "Across two devices"), Instant.now()) { "Capture" }
        a.repository.saveItem(item)
        assertEquals(SyncResult.DONE, a.syncRunner.run())
        assertEquals(SyncResult.DONE, b.syncRunner.run())
        assertTrue(b.repository.state.value.items.any { it.id == item.id && it.title == item.title })
        b.repository.saveItem(item.copy(title = "Edited on device B"))
        assertEquals(SyncResult.DONE, b.syncRunner.run())
        assertEquals(SyncResult.DONE, a.syncRunner.run())
        assertEquals("Edited on device B", a.repository.state.value.items.single { it.id == item.id }.title)
        a.accountService.deleteAccount(password)
    }

    @Test fun temporaryFailureKeepsOutboxAndRecovers() = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("testServer") ?: error("testServer is required")
        require(URI(url).host == "10.0.2.2")
        val a = AppContainer(Installation(ApplicationProvider.getApplicationContext(), "recovery"), backgroundWork = false)
        a.repository.load()
        a.accountService.signIn(url, "retry-${UUID.randomUUID()}@example.com", "device test password", true, "en")
        val account = requireNotNull(a.accounts.account.value)
        val tokens = Tokens(requireNotNull(a.accounts.accessToken), requireNotNull(a.accounts.refreshToken))
        a.repository.saveItem(CaptureFactory.create("offline", CaptureInput(CaptureKind.TEXT, "Never lose this"), Instant.now()) { "Capture" })
        a.accounts.signedIn(account.copy(serverUrl = "http://10.0.2.2:8081"), tokens)
        assertEquals(SyncResult.RETRY, a.syncRunner.run())
        assertTrue(a.repository.state.value.outbox.queued.isNotEmpty())
        a.accounts.signedIn(account, tokens)
        assertEquals(SyncResult.DONE, a.syncRunner.run())
        assertTrue(a.repository.state.value.outbox.queued.isEmpty())
        a.accountService.deleteAccount("device test password")
    }
}

private class Installation(base: Context, private val prefix: String) : ContextWrapper(base) {
    override fun getApplicationContext(): Context = this
    override fun getDatabasePath(name: String): File = super.getDatabasePath("$prefix-$name")
    override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("$prefix-$name", mode)
    override fun getFilesDir(): File = File(super.getFilesDir(), prefix).apply { mkdirs() }
    override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
        super.openOrCreateDatabase("$prefix-$name", mode, factory)
    override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, handler: DatabaseErrorHandler?): SQLiteDatabase =
        super.openOrCreateDatabase("$prefix-$name", mode, factory, handler)
}
