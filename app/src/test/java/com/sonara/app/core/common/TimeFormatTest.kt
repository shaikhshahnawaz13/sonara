package com.sonara.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun underAMinute() = assertEquals("0:07", formatDuration(7_400))
    @Test fun minutesAndSeconds() = assertEquals("3:05", formatDuration(185_000))
    @Test fun overAnHour() = assertEquals("1:02:03", formatDuration(3_723_000))
    @Test fun zeroAndNegativeShowZero() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5_000))
    }
    @Test fun truncatesPartialSeconds() = assertEquals("0:59", formatDuration(59_999))

    @Test fun totalDurationRoundsToMinutes() {
        assertEquals("0 MIN", formatTotalDuration(0))
        assertEquals("1 MIN", formatTotalDuration(20_000))
        assertEquals("45 MIN", formatTotalDuration(45 * 60_000L))
        assertEquals("1 H 05 MIN", formatTotalDuration(65 * 60_000L))
        assertEquals("2 H 00 MIN", formatTotalDuration(120 * 60_000L))
    }
}
