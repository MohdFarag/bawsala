package app.bawsala.android.domain

/** Recognizes links from Google apps so a share keeps its real title and a link, not a copy. */
object SourceLinks {
    private val YOUTUBE_ID =
        Regex(
            """(?:youtube\.com/(?:watch\?(?:.*&)?v=|shorts/|live/|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})"""
        )
    private val DRIVE =
        Regex("""https://(?:drive|docs)\.google\.com/\S+""", RegexOption.IGNORE_CASE)

    /** The 11-character video id of a YouTube link, or null. */
    fun youTubeId(url: String?): String? = url?.let { YOUTUBE_ID.find(it)?.groupValues?.get(1) }

    /** YouTube's still image for a video (480×360). */
    fun youTubeThumbnail(id: String) = "https://i.ytimg.com/vi/$id/hqdefault.jpg"

    /**
     * The video title from YouTube's share subject, which wraps it ("Watch “Title” on YouTube", or
     * in Arabic "شاهِد "Title" على YouTube"); a plain subject is kept as it is.
     */
    fun youTubeTitle(subject: String?): String? {
        val s = subject?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val quoted = Regex("""[“"«]([^”"»]+)[”"»]""").find(s)?.groupValues?.get(1)
        return (quoted ?: s).trim()
    }

    /** A Google Drive or Docs link in [text], or null. */
    fun driveLink(text: String?): String? = text?.let { DRIVE.find(it)?.value }
}
