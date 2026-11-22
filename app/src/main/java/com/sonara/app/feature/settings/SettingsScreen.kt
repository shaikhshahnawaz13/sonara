package com.sonara.app.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.audio.EqualizerLogic
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraConfirmDialog
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.core.permissions.LocalPermissionActions
import com.sonara.app.data.local.preferences.AnimationIntensity
import com.sonara.app.data.local.preferences.LibraryPrefs
import com.sonara.app.data.local.preferences.PlaybackPrefs
import com.sonara.app.data.local.preferences.SonaraSettings
import com.sonara.app.data.local.preferences.SongSort
import com.sonara.app.data.repository.SyncState
import com.sonara.app.data.storage.ByteFormat
import com.sonara.app.data.storage.StorageReport
import com.sonara.app.feature.common.appViewModel
import kotlinx.coroutines.launch

private enum class SettingsDialog { ClearArtwork, ClearCache }

private val animationLevels = listOf(AnimationIntensity.Full, AnimationIntensity.Reduced, AnimationIntensity.Off)

/**
 * Settings: playback, audio, appearance, library, storage, notifications and about. Every control here changes
 * something real; capabilities the platform does not offer (gapless switch, scan depth) are explained, not faked.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val vm = appViewModel { SettingsViewModel(it, context) }
    val navigator = LocalSonaraNavigator.current
    val snackbar = LocalSnackbar.current
    val permissions = LocalPermissionActions.current
    val scope = rememberCoroutineScope()

    val settings by vm.prefsFlow.collectAsStateWithLifecycle(initialValue = SonaraSettings())
    val playback by vm.playbackPrefs.collectAsStateWithLifecycle(initialValue = PlaybackPrefs())
    val library by vm.libraryPrefs.collectAsStateWithLifecycle(initialValue = LibraryPrefs())
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val audioAccess by vm.audioAccess.collectAsStateWithLifecycle()
    val equalizer by LocalAppContainer.current.audioEffects.state.collectAsStateWithLifecycle()

    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    // The person may change this in system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    var storageVersion by remember { mutableIntStateOf(0) }
    var storage by remember { mutableStateOf<StorageReport?>(null) }
    LaunchedEffect(storageVersion) { storage = vm.measureStorage() }

    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val build = remember { buildInfo(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "SETTINGS", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            SettingsSectionHeading("PLAYBACK", "How your music starts and carries on.")
            SettingsSwitchRow(
                title = "Remember queue and position",
                description = "Reopen SONARA where you left off. The last queue and position come back paused.",
                checked = settings.resumePlayback,
                onCheckedChange = { vm.setResume(it) },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "Play next automatically",
                description = "When off, playback pauses at the end of each song and waits for you.",
                checked = playback.autoPlayNext,
                onCheckedChange = { vm.setAutoPlayNext(it) },
            )
            SettingsDivider()
            SettingsNote("Songs play gaplessly wherever the file allows it. Android decides this, so there is nothing to switch.")

            SettingsSectionHeading("AUDIO", "Shape the sound.")
            SettingsActionRow(
                title = "Equalizer",
                description = EqualizerLogic.statusText(equalizer),
                onClick = navigator::openEqualizer,
                showsMore = true,
            )

            SettingsSectionHeading("APPEARANCE", "Colour, motion and touch.")
            SettingsAccentRow(selected = settings.accent, onSelect = { vm.setAccent(it) })
            SettingsDivider()
            SettingsChoiceRow(
                title = "Animation",
                description = "Full uses all motion, Reduced shortens it, Off removes it. SONARA also follows Android's animation setting.",
                options = animationLevels.map { it.name },
                selectedIndex = animationLevels.indexOf(settings.animationIntensity).coerceAtLeast(0),
                onSelect = { vm.setAnimation(animationLevels[it]) },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = "Haptics",
                description = "A light vibration when you reorder a playlist, move equalizer bands or change a setting.",
                checked = settings.hapticsEnabled,
                onCheckedChange = { vm.setHaptics(it) },
            )
            SettingsDivider()
            SettingsNote("SONARA has one dark theme.")

            SettingsSectionHeading("LIBRARY", "Where your music comes from.")
            val scanning = syncState is SyncState.Scanning
            val canScan = audioAccess == AudioAccessState.Granted && !scanning
            SettingsActionRow(
                title = "Rescan library",
                description = when {
                    audioAccess != AudioAccessState.Granted -> "Music access is needed before SONARA can scan."
                    else -> SettingsLogic.scanResultText(syncState) ?: "Look for new, changed and removed songs now."
                },
                value = if (scanning) "SCANNING" else "SCAN",
                enabled = canScan,
                onClick = { vm.rescan() },
            )
            SettingsDivider()
            SettingsInfoRow(
                title = "Last scan",
                value = SettingsLogic.lastScanText(library.lastScanAt, System.currentTimeMillis()),
            )
            SettingsDivider()
            SettingsActionRow(
                title = "Excluded folders",
                description = "Keep chosen folders out of your library.",
                value = SettingsLogic.excludedCountText(library.excludedFolders.size),
                onClick = navigator::openExcludedFolders,
                showsMore = true,
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = "Default sort for Songs",
                description = "Title is A to Z. Newest puts the most recently added first.",
                options = SongSort.entries.map { it.label },
                selectedIndex = SongSort.entries.indexOf(library.defaultSort).coerceAtLeast(0),
                onSelect = { vm.setSort(SongSort.entries[it]) },
            )
            SettingsDivider()
            SettingsNote("SONARA reads your whole device library from Android, so there is no subfolder option to set.")

            SettingsSectionHeading("STORAGE", "What SONARA keeps on this device.")
            SettingsInfoRow("Library database", storage?.let { ByteFormat.format(it.libraryDatabaseBytes) } ?: "Measuring…")
            SettingsDivider()
            SettingsInfoRow("Artwork in memory", storage?.let { ByteFormat.format(it.artworkMemoryBytes) } ?: "Measuring…")
            SettingsDivider()
            SettingsInfoRow("App cache", storage?.let { ByteFormat.format(it.cacheBytes) } ?: "Measuring…")
            SettingsDivider()
            SettingsActionRow(
                title = "Clear artwork cache",
                description = "Frees decoded album art from memory. It loads again as you browse.",
                onClick = { dialog = SettingsDialog.ClearArtwork },
            )
            SettingsDivider()
            SettingsActionRow(
                title = "Clear app cache",
                description = "Removes temporary files SONARA created.",
                onClick = { dialog = SettingsDialog.ClearCache },
            )
            SettingsDivider()
            SettingsNote("Your music files, playlists, Favourite and settings are never touched by these.")

            SettingsSectionHeading("NOTIFICATIONS", "The playback notification and lock screen.")
            SettingsActionRow(
                title = "Playback notification",
                description = SettingsLogic.notificationStatus(notificationsOn),
                value = "OPEN",
                onClick = { openNotificationSettings(context) },
            )
            SettingsDivider()
            SettingsNote("While a song is loaded, its controls appear in the notification and on the lock screen. Android decides whether the lock screen shows notifications.")
            SettingsDivider()
            SettingsActionRow(
                title = "Music access",
                description = when (audioAccess) {
                    AudioAccessState.Granted -> "Allowed"
                    else -> "Not allowed. SONARA cannot see your songs without it."
                },
                value = if (audioAccess == AudioAccessState.Granted) null else "FIX",
                enabled = audioAccess != AudioAccessState.Granted,
                onClick = permissions.openAppSettings,
            )

            SettingsSectionHeading("ABOUT", "SONARA, an offline music player.")
            SettingsInfoRow("Version", SettingsLogic.versionText(build.versionName, build.versionCode, build.debuggable))
            SettingsDivider()
            SettingsActionRow(
                title = "Open-source licences",
                description = "The projects SONARA is built with.",
                onClick = navigator::openLicenses,
                showsMore = true,
            )
            SettingsDivider()
            SettingsNote(OpenSourceNotices.ACKNOWLEDGEMENT)
            Spacer(Modifier.height(SonaraSpacing.s32))
        }
    }

    when (dialog) {
        SettingsDialog.ClearArtwork -> SonaraConfirmDialog(
            title = "CLEAR ARTWORK CACHE?",
            body = "Decoded album art held in memory is released and loaded again when needed. Nothing on your device is deleted.",
            confirmLabel = "CLEAR",
            onConfirm = {
                dialog = null
                val freed = vm.clearArtwork()
                snackbar.show(SettingsLogic.clearedText(freed, "artwork cache"))
                storageVersion++
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ClearCache -> SonaraConfirmDialog(
            title = "CLEAR APP CACHE?",
            body = "Temporary files SONARA created are removed. Your music, playlists, Favourite, library and settings are not affected.",
            confirmLabel = "CLEAR",
            onConfirm = {
                dialog = null
                scope.launch {
                    val freed = vm.clearCacheDir()
                    snackbar.show(SettingsLogic.clearedText(freed, "app cache"))
                    storageVersion++
                }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private class BuildInfo(val versionName: String?, val versionCode: Long, val debuggable: Boolean)

@Suppress("DEPRECATION")
private fun buildInfo(context: Context): BuildInfo {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return BuildInfo(
        versionName = info.versionName,
        versionCode = info.longVersionCode,
        debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
    )
}

/** Opens Android's own notification settings for SONARA. If the system has no such screen, nothing happens. */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
