package com.sonara.app.core.permissions

import com.sonara.app.data.local.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Process-wide view of audio access. The Activity reports the facts only it can know (rationale state);
 * everything else (library sync, UI) observes [state] and never touches an Activity.
 */
class AudioAccessModel(
    private val checker: AudioPermissionChecker,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        if (checker.isGranted()) AudioAccessState.Granted else AudioAccessState.NotRequested,
    )
    val state: StateFlow<AudioAccessState> = _state

    /** Recomputes the state. [shouldShowRationale] comes from the Activity; null means "unknown" (no Activity). */
    fun refresh(shouldShowRationale: Boolean?) {
        scope.launch {
            val granted = checker.isGranted()
            if (granted) {
                _state.value = AudioAccessState.Granted
                return@launch
            }
            val requested = settings.settings.first().audioPermissionRequested
            _state.value = if (shouldShowRationale == null) {
                if (requested) AudioAccessState.Denied else AudioAccessState.NotRequested
            } else {
                AudioPermissionPolicy.resolve(
                    granted = false,
                    requestedBefore = requested,
                    shouldShowRationale = shouldShowRationale,
                )
            }
        }
    }
}
