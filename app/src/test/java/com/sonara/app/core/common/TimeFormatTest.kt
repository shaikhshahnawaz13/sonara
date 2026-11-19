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
}
