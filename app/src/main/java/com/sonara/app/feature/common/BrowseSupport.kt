package com.sonara.app.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sonara.app.app.AppContainer
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraMenuAction
import com.sonara.app.core.design.components.SonaraSnackbarState
import com.sonara.app.core.navigation.SonaraNavigator
import com.sonara.app.data.local.database.TrackEntity
import com.sonara.app.playback.PlaybackController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What a screen's data is doing right now. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Base for the browsing ViewModels: turns repository flows into [UiState] and supports retrying a failed load. */
abstract class BrowseViewModel : ViewModel() {
    private val retryTick = MutableStateFlow(0)

    fun retry() {
        retryTick.value += 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> Flow<T>.asUiState(): StateFlow<UiState<T>> {
        val source = this
        return retryTick
            .flatMapLatest {
                source
                    .map<T, UiState<T>> { UiState.Content(it) }
                    .catch { emit(UiState.Error(it.message ?: "Unexpected error")) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Creates a ViewModel that needs the app's repositories. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

/**
 * Playback commands for the UI, with the confirmation message each one deserves.
 * It holds nothing but the controller and the snackbar; the playback service owns the queue.
 */
@Stable
class PlaybackActions(
    private val controller: PlaybackController,
    private val snackbar: SonaraSnackbarState,
) {
    fun play(ids: List<Long>, index: Int) {
        if (!controller.playTracks(ids, index)) notReady()
    }

    fun shuffle(ids: List<Long>) {
        if (!controller.playShuffled(ids)) notReady()
    }

    fun playNext(ids: List<Long>, what: String) {
        if (controller.playNext(ids)) snackbar.show("PLAYING NEXT · $what") else notReady()
    }

    fun addToQueue(ids: List<Long>, what: String) {
        if (controller.addToQueue(ids)) snackbar.show("ADDED TO QUEUE · $what") else notReady()
    }

    private fun notReady() = snackbar.show("PLAYBACK IS NOT READY YET")
}

@Composable
fun rememberPlaybackActions(): PlaybackActions {
    val controller = LocalAppContainer.current.playbackController
    val snackbar = LocalSnackbar.current
    return remember(controller, snackbar) { PlaybackActions(controller, snackbar) }
}

/** The overflow actions of a song row. Entries that would open the screen already showing are left out. */
fun trackMenu(
    track: TrackEntity,
    actions: PlaybackActions,
    navigator: SonaraNavigator,
    showAlbum: Boolean = true,
    showArtist: Boolean = true,
): List<SonaraMenuAction> = buildList {
    add(SonaraMenuAction("PLAY NEXT") { actions.playNext(listOf(track.id), track.title) })
    add(SonaraMenuAction("ADD TO QUEUE") { actions.addToQueue(listOf(track.id), track.title) })
    if (showAlbum) add(SonaraMenuAction("VIEW ALBUM") { navigator.openAlbum(track.albumId) })
    if (showArtist) add(SonaraMenuAction("VIEW ARTIST") { navigator.openArtist(track.artist) })
}

/**
 * The id of the track playing now, as a State that changes only when the track changes. Reading the whole
 * playback state inside a list would recompose every visible row on each play/pause or buffering flip.
 */
@Composable
fun rememberCurrentTrackId(): State<Long?> {
    val controller = LocalAppContainer.current.playbackController
    val playback = controller.state.collectAsStateWithLifecycle()
    return remember(controller) { derivedStateOf { playback.value.currentTrackId } }
}

/** The overflow actions of a song row, remembered so an unchanged row keeps the same list instance. */
@Composable
fun rememberTrackMenu(
    track: TrackEntity,
    actions: PlaybackActions,
    navigator: SonaraNavigator,
    showAlbum: Boolean = true,
    showArtist: Boolean = true,
): List<SonaraMenuAction> =
    remember(track, actions, navigator, showAlbum, showArtist) {
        trackMenu(track, actions, navigator, showAlbum, showArtist)
    }
