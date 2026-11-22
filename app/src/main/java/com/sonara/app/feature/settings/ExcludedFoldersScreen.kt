package com.sonara.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraConfirmDialog
import com.sonara.app.core.design.components.SonaraDialog
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.data.local.preferences.LibraryPrefs
import com.sonara.app.data.model.Folder
import com.sonara.app.data.repository.FolderExclusions
import com.sonara.app.feature.common.appViewModel

/**
 * Folders kept out of the library. Excluding hides their songs from Songs, Albums, Artists, Search and the other
 * browse screens. It never deletes a file, and it never removes a song from a playlist or from Favourite: those
 * entries show as unavailable and come back by themselves when the folder is included again.
 */
@Composable
fun ExcludedFoldersScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val vm = appViewModel { SettingsViewModel(it, context) }
    val snackbar = LocalSnackbar.current
    val prefs by vm.libraryPrefs.collectAsStateWithLifecycle(initialValue = LibraryPrefs())
    val folders by vm.libraryFolders.collectAsStateWithLifecycle(initialValue = emptyList())

    var picking by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<Folder?>(null) }
    val excluded = prefs.excludedFolders.sorted()

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "EXCLUDED FOLDERS", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsNote(
                "Songs in an excluded folder, and in the folders inside it, are kept out of your library. " +
                    "They stay in your playlists and Favourite as unavailable, and return when you include the folder again. " +
                    "No audio file is ever deleted.",
            )
            SettingsSectionHeading("EXCLUDED", "Hidden from your library.")
            if (excluded.isEmpty()) {
                SettingsNote("No folders are excluded.")
            }
            excluded.forEachIndexed { index, path ->
                if (index > 0) SettingsDivider()
                ExcludedRow(path = path, onInclude = {
                    vm.include(path)
                    snackbar.show("Including ${FolderExclusions.displayName(path)}")
                })
            }
            SettingsDivider()
            Row(modifier = Modifier.padding(SonaraSpacing.s16)) {
                SonaraButton(label = "EXCLUDE A FOLDER", onClick = { picking = true }, emphasized = true)
            }
        }
    }

    if (picking) {
        val candidates = remember(folders, prefs.excludedFolders) {
            folders.filterNot { FolderExclusions.isExcluded(it.path, prefs.excludedFolders) }
        }
        SonaraDialog(title = "CHOOSE A FOLDER", onDismiss = { picking = false }) {
            if (candidates.isEmpty()) {
                Text("No other folders in your library.", style = SonaraTheme.typography.body, color = SonaraTheme.colors.foregroundMuted)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(candidates, key = { it.path }) { folder ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    picking = false
                                    confirming = folder
                                }
                                .padding(vertical = SonaraSpacing.s8),
                        ) {
                            Text(folder.name, style = SonaraTheme.typography.trackTitle, color = SonaraTheme.colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${folder.path} · ${SettingsLogic.songs(folder.trackCount)}",
                                style = SonaraTheme.typography.metadata,
                                color = SonaraTheme.colors.foregroundMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            SonaraButton(label = "CANCEL", onClick = { picking = false })
        }
    }

    confirming?.let { folder ->
        SonaraConfirmDialog(
            title = "EXCLUDE ${folder.name.uppercase()}?",
            body = "${SettingsLogic.songs(folder.trackCount)} in this folder and its subfolders will leave your library. " +
                "They stay in your playlists and Favourite as unavailable, and come back if you include the folder again. " +
                "No file is deleted.",
            confirmLabel = "EXCLUDE",
            onConfirm = {
                confirming = null
                vm.exclude(folder.path)
                snackbar.show("Excluding ${folder.name}")
            },
            onDismiss = { confirming = null },
        )
    }
}

@Composable
private fun ExcludedRow(path: String, onInclude: () -> Unit) {
    val colors = SonaraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(start = SonaraSpacing.s16, end = SonaraSpacing.s8, top = SonaraSpacing.s8, bottom = SonaraSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonaraSpacing.s12),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(FolderExclusions.displayName(path), style = SonaraTheme.typography.trackTitle, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(path, style = SonaraTheme.typography.metadata, color = colors.foregroundMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SonaraButton(label = "INCLUDE", onClick = onInclude)
    }
}
