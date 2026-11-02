package app.bawsala.android.domain

import app.bawsala.android.data.Codec
import app.bawsala.android.data.Snapshot
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/** A documented, restorable JSON export of everything stored on the device (SEC02). */
object DataExport {
    const val FORMAT = "bawsala/1"

    fun toJson(snapshot: Snapshot, exportedAt: Instant): String =
        JSONObject()
            .put("format", FORMAT)
            .put("exportedAt", exportedAt.toString())
            .put("items", JSONArray(snapshot.items.map(Codec::encode)))
            .put("tasks", JSONArray(snapshot.tasks.map(Codec::encode)))
            .put("lists", JSONArray(snapshot.lists.map(Codec::encode)))
            .put("projects", JSONArray(snapshot.projects.map(Codec::encode)))
            .put("areas", JSONArray(snapshot.areas.map(Codec::encode)))
            .put("sessions", JSONArray(snapshot.sessions.map(Codec::encode)))
            .put("rules", JSONArray(snapshot.rules.map(Codec::encode)))
            .put("blocks", JSONArray(snapshot.blocks.map(Codec::encode)))
            .put("prayer", Codec.encode(snapshot.prayer))
            .put("timeLog", Codec.encode(snapshot.timeLog))
            .put("notIncluded", "Attachment binaries are referenced by path and not embedded.")
            .toString(2)
}
