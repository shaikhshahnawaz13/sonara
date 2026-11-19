package com.sonara.app.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sonara.app.R
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.ArtworkLoader
import com.sonara.app.core.design.components.LocalArtworkLoader
import com.sonara.app.core.navigation.SonaraApp
import com.sonara.app.core.permissions.LocalPermissionActions
import com.sonara.app.core.permissions.PermissionCoordinator
import com.sonara.app.data.local.preferences.SonaraSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var permissions: PermissionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest theme carries the system splash screen; switch to the normal theme once it is shown.
        setTheme(R.style.Theme_Sonara)
        super.onCreate(savedInstanceState)

        // SONARA is always dark, so the system bars always use light icons regardless of the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        container = (application as SonaraApplication).container
        // Launchers must be registered before the Activity is STARTED.
        permissions = PermissionCoordinator(
            activity = this,
            audioAccess = container.audioAccess,
            settings = container.settingsRepository,
            checker = container.audioPermissionChecker,
            scope = container.appScope,
        )

        val artworkProvider = container.artworkProvider
        val artworkLoader = ArtworkLoader { trackId, sizePx -> artworkProvider.load(trackId, sizePx) }

        setContent {
            val settings by container.settingsRepository.settings.collectAsState(initial = SonaraSettings())
            val motionLevel = resolveMotionLevel(settings.animationIntensity, rememberSystemAnimatorScale())

            SonaraTheme(motionLevel = motionLevel) {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalPermissionActions provides permissions.actions,
                    LocalArtworkLoader provides artworkLoader,
                ) {
                    SonaraApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Playback lives in the service; the Activity only attaches a controller while it is visible.
        container.playbackController.connect()
    }

    override fun onResume() {
        super.onResume()
        // The permission may have been changed in system Settings while the app was in the background.
        permissions.refresh()
        // Saved folders may have lost their grant, or their storage, while the app was away.
        container.appScope.launch { container.folderRepository.refreshStates() }
    }

    override fun onStop() {
        container.playbackController.disconnect()
        super.onStop()
    }
}
