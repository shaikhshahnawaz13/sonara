package com.sonara.app.feature.folders

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.sonara.app.data.model.Folder
import com.sonara.app.data.repository.FolderRepository
import com.sonara.app.data.repository.LibraryRepository
import com.sonara.app.data.repository.UserFolder
import com.sonara.app.feature.common.BrowseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FoldersContent(
    val userFolders: List<UserFolder>,
    val indexedFolders: List<Folder>,
)

class FoldersViewModel(
    private val folders: FolderRepository,
    private val library: LibraryRepository,
) : BrowseViewModel() {

    val content = combine(folders.userFolders, folders.indexedFolders) { user, indexed ->
        FoldersContent(user, indexed)
    }.asUiState()

    val syncState = library.syncState

    init {
        refreshStates()
    }

    fun refreshStates() {
        viewModelScope.launch { folders.refreshStates() }
    }

    fun addFolder(treeUri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(folders.add(treeUri)) }
    }

    fun removeFolder(id: Long) {
        viewModelScope.launch { folders.remove(id) }
    }

    fun rescan() {
        viewModelScope.launch { library.sync(force = true) }
    }
}
