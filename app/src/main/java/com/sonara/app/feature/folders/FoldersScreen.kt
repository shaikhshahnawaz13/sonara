package com.sonara.app.feature.folders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.sonara.app.app.LocalAppContainer
import com.sonara.app.core.design.SonaraSpacing
import com.sonara.app.core.design.SonaraTheme
import com.sonara.app.core.design.components.LocalSnackbar
import com.sonara.app.core.design.components.SonaraButton
import com.sonara.app.core.design.components.SonaraErrorState
import com.sonara.app.core.design.components.SonaraFolderRow
import com.sonara.app.core.design.components.SonaraIconButton
import com.sonara.app.core.design.components.SonaraLoadingState
import com.sonara.app.core.design.components.SonaraMenuAction
import com.sonara.app.core.design.components.SonaraOverflowMenu
import com.sonara.app.core.design.components.SonaraPermissionPrompt
import com.sonara.app.core.design.components.SonaraSectionHeader
import com.sonara.app.core.design.components.SonaraTopBar
import com.sonara.app.core.navigation.LocalSonaraNavigator
import com.sonara.app.core.permissions.AudioAccessState
import com.sonara.app.data.local.database.FolderPermissionState
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.feature.common.LibraryEmptyState
import com.sonara.app.feature.common.UiState
import com.sonara.app.feature.common.appViewModel
import com.sonara.app.core.design.components.SonaraIcons

@Composable
fun FoldersScreen() {
    val container = LocalAppContainer.current
    val navigator = LocalSonaraNavigator.current
    val snackbar = LocalSnackbar.current
    val viewModel = appViewModel { FoldersViewModel(it.folderRepository, it.libraryRepository) }
    val access by container.audioAccess.state.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()
    val colors = SonaraTheme.colors

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.addFolder(uri) { result ->
                snackbar.show(
                    if (result.isSuccess) "FOLDER ADDED" else "COULD NOT KEEP ACCESS TO THAT FOLDER",
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(
            title = "FOLDERS",
            actions = {
                SonaraIconButton(SonaraIcons.Add, "Add a folder", onClick = { picker.launch(null) })
            },
        )
        when (val state = content) {
            UiState.Loading -> SonaraLoadingState()
            is UiState.Error -> SonaraErrorState(state.message, onRetry = viewModel::retry)
            is UiState.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                val granted = access == AudioAccessState.Granted
                if (!granted) item { SonaraPermissionPrompt(access) }

                item {
                    SonaraSectionHeader("YOUR FOLDERS", Modifier.padding(horizontal = SonaraSpacing.s16))
                }
                if (state.data.userFolders.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = SonaraSpacing.s16)) {
                            Text(
                                "Folders you choose with the system picker appear here. SONARA only reads folders you grant, and only plays files the library scan has indexed.",
                                style = SonaraTheme.typography.body,
                                color = colors.foregroundMuted,
                            )
                            SonaraButton(
                                label = "ADD A FOLDER",
                                onClick = { picker.launch(null) },
                                emphasized = true,
                                modifier = Modifier.padding(top = SonaraSpacing.s12),
                            )
                        }
                    }
                }
                items(items = state.data.userFolders, key = { "u-${it.id}" }, contentType = { "user-folder" }) { folder ->
                    val usable = folder.state == FolderPermissionState.Granted
                    SonaraFolderRow(
                        name = folder.name,
                        location = folder.location.path.ifEmpty { folder.location.volume },
                        trackCount = folder.indexedTrackCount,
                        status = when (folder.state) {
                            FolderPermissionState.Granted -> null
                            FolderPermissionState.Revoked -> "ACCESS REVOKED · REMOVE AND ADD IT AGAIN"
                            FolderPermissionState.Unavailable -> "FOLDER MOVED OR STORAGE REMOVED"
                        },
                        statusIsProblem = !usable,
                        onClick = {
                            val prefix = folder.location.relativePrefix
                            navigator.openFolder(FolderRepository.routePath(prefix), recursive = true)
                        },
                        trailing = {
                            SonaraOverflowMenu(
                                actions = listOf(
                                    SonaraMenuAction("REMOVE FOLDER") { viewModel.removeFolder(folder.id) },
                                ),
                                contentDescription = "Actions for ${folder.name}",
                            )
                        },
                    )
                }

                item {
                    SonaraSectionHeader("INDEXED FOLDERS", Modifier.padding(horizontal = SonaraSpacing.s16))
                }
                if (granted) {
                    if (state.data.indexedFolders.isEmpty()) {
                        item { LibraryEmptyState(sync = sync, onRescan = viewModel::rescan) }
                    } else {
                        items(items = state.data.indexedFolders, key = { "i-${it.path}" }, contentType = { "folder" }) { folder ->
                            SonaraFolderRow(
                                name = folder.name,
                                location = folder.parentPath,
                                trackCount = folder.trackCount,
                                onClick = { navigator.openFolder(folder.path, recursive = false) },
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Allow audio access to see the folders SONARA has found.",
                            style = SonaraTheme.typography.body,
                            color = colors.foregroundMuted,
                            modifier = Modifier.padding(horizontal = SonaraSpacing.s16),
                        )
                    }
                }
            }
        }
    }
}
