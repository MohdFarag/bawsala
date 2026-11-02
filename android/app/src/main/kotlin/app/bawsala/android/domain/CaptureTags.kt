package app.bawsala.android.domain

/**
 * Details noticed in what the user typed, with simple text rules (no AI, decision 2 of the V3
 * plan): a day word, an hour, a place or context, and explicit #hashtags. Shown as removable chips
 * in quick capture and kept on the item for triage.
 */
object CaptureTags {
    /** Whole Arabic or English words only (so "غد" does not match inside "بغداد"). */
    private fun words(vararg w: String) =
        Regex("""(?<![\p{L}])(?:${w.joinToString("|")})(?![\p{L}])""", RegexOption.IGNORE_CASE)

    private val TOMORROW = words("بكرة", "بكره", "غدًا", "غداً", "غدا", "غد", "tomorrow")
    private val TODAY = words("النهارده", "النهاردة", "الليلة", "today", "tonight")
    private val HOUR =
        Regex(
            """(?:الساعة|الساعه|\bat)\s*([0-9٠-٩]{1,2})(?:[:.]([0-9٠-٩]{2}))?""",
            RegexOption.IGNORE_CASE,
        )
    private val HOME = words("المنزل", "منزل", "البيت", "مشتريات", "home", "groceries")
    private val OUTSIDE =
        words("موعد", "أسنان", "دكتور", "طبيب", "appointment", "dentist", "doctor")
    private val COMPUTER = words("إيميل", "ايميل", "بريد", "email", "laptop", "computer")
    private val HASHTAG = Regex("""#([\p{L}\p{N}_]+)""")

    /** Tags for [text] in [lang] ("ar" or "en"), in a stable order, without duplicates. */
    fun detect(text: String, lang: String): List<String> {
        val ar = lang == "ar"
        val tags = mutableListOf<String>()
        if (TOMORROW.containsMatchIn(text)) tags += if (ar) "غدًا" else "Tomorrow"
        else if (TODAY.containsMatchIn(text)) tags += if (ar) "اليوم" else "Today"
        HOUR.find(text)?.let { m ->
            val h = latinDigits(m.groupValues[1]).toInt()
            val min = m.groupValues[2].takeIf { it.isNotEmpty() }?.let(::latinDigits) ?: "00"
            if (h in 0..23) tags += time(h, min, ar)
        }
        if (HOME.containsMatchIn(text)) tags += if (ar) "@المنزل" else "@home"
        if (OUTSIDE.containsMatchIn(text)) tags += if (ar) "@خارج المنزل" else "@outside"
        if (COMPUTER.containsMatchIn(text)) tags += if (ar) "@على الكمبيوتر" else "@computer"
        HASHTAG.findAll(text).forEach { tags += "#" + it.groupValues[1] }
        return tags.distinct()
    }

    /** "6:00 PM" style; an hour under 12 without a period is read as the afternoon from 1 to 7. */
    private fun time(hour: Int, minutes: String, ar: Boolean): String {
        val evening = hour in 1..7 || hour >= 12
        val h12 = ((hour + 11) % 12) + 1
        val clock = "$h12:$minutes"
        return if (ar) toArabicDigits(clock) + if (evening) " مساءً" else " صباحًا"
        else clock + if (evening) " PM" else " AM"
    }

    private fun latinDigits(s: String) =
        s.map { if (it in '٠'..'٩') '0' + (it - '٠') else it }.joinToString("")

    private fun toArabicDigits(s: String) =
        s.map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")
}
