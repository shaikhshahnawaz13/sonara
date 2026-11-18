package com.sonara.app.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sonara.app.R
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.navigation.SonaraApp
import com.sonara.app.data.local.preferences.SonaraSettings

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest theme carries the system splash screen; switch to the normal theme once it is shown.
        setTheme(R.style.Theme_Sonara)
        super.onCreate(savedInstanceState)

        // SONARA is always dark, so the system bars always use light icons regardless of the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val container = (application as SonaraApplication).container

        setContent {
            val settings by container.settingsRepository.settings.collectAsState(initial = SonaraSettings())
            val motionLevel = resolveMotionLevel(settings.animationIntensity, rememberSystemAnimatorScale())

            SonaraTheme(motionLevel = motionLevel) {
                SonaraApp()
            }
        }
    }
}
