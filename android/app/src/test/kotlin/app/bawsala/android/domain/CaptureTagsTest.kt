package app.bawsala.android.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureTagsTest {
    @Test
    fun `the prototype's example gives tomorrow, 6 pm and outside`() {
        assertEquals(
            listOf("غدًا", "٦:٠٠ مساءً", "@خارج المنزل"),
            CaptureTags.detect("أحجز موعد أسنان بكرة الساعة ٦.", "ar"),
        )
    }

    @Test
    fun `english text, latin digits and minutes`() {
        assertEquals(
            listOf("Tomorrow", "9:30 AM", "@home"),
            CaptureTags.detect("Groceries tomorrow at 9:30", "en"),
        )
        assertEquals(listOf("Today", "2:00 PM"), CaptureTags.detect("Call today at 14", "en"))
    }

    @Test
    fun `hashtags are kept and nothing is invented`() {
        assertEquals(listOf("#بوصلة"), CaptureTags.detect("أكتب خطة #بوصلة", "ar"))
        assertEquals(emptyList(), CaptureTags.detect("فكرة لهدية نهاية الأسبوع.", "ar"))
    }
}
