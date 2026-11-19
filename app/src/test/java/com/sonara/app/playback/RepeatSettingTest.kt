package com.sonara.app.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatSettingTest {
    @Test
    fun cyclesOffAllOneOff() {
        assertEquals(RepeatSetting.All, RepeatSetting.Off.next())
        assertEquals(RepeatSetting.One, RepeatSetting.All.next())
        assertEquals(RepeatSetting.Off, RepeatSetting.One.next())
    }

    @Test
    fun mapsToAndFromPlayerConstants() {
        assertEquals(Player.REPEAT_MODE_ALL, RepeatSetting.All.playerValue)
        assertEquals(RepeatSetting.One, RepeatSetting.fromPlayerValue(Player.REPEAT_MODE_ONE))
        assertEquals(RepeatSetting.Off, RepeatSetting.fromPlayerValue(99))
    }
}
