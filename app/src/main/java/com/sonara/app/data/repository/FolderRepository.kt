package com.sonara.app.data.repository

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.sonara.app.data.local.database.FolderPermissionState
import com.sonara.app.data.local.database.SelectedFolderEntity
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.model.Folder
import com.sonara.app.data.model.FolderNames
import com.sonara.app.data.model.TreeLocation
import com.sonara.app.data.model.TreeUris
import com.sonara.app.data.model.toFolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** A folder the person granted through the system folder picker. */
data class UserFolder(
    val id: Long,
    val treeUri: String,
    val name: String,
    val location: TreeLocation,
    val state: FolderPermissionState,
    /** Tracks SONARA has indexed inside this folder and its subfolders. */
    val indexedTrackCount: Int,
)

/**
 * Folders: those found by the MediaStore scan (derived from track paths) and those the person granted
 * through the Storage Access Framework. SONARA never claims access beyond these.
 */
class FolderRepository(
    database: SonaraDatabase,
    private val resolver: ContentResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val trackDao = database.trackDao()
    private val folderDao = database.selectedFolderDao()

    val indexedFolders: Flow<List<Folder>> = trackDao.observeFolders().map { rows -> rows.map { it.toFolder() } }

    val userFolders: Flow<List<UserFolder>> =
        combine(folderDao.observeAll(), trackDao.observeCount()) { folders, _ -> folders }
            .map { folders ->
                folders.map { entity ->
                    val location = TreeUris.parse(documentIdOrEmpty(entity.treeUri))
                    val prefix = location.relativePrefix
                    UserFolder(
                        id = entity.id,
                        treeUri = entity.treeUri,
                        name = entity.displayName,
                        location = location,
                        state = entity.permissionState,
                        indexedTrackCount = trackDao.countTree(prefix, prefix.length),
                    )
                }
            }

    /** Remembers a folder returned by the system picker and keeps read access across restarts. */
    suspend fun add(treeUri: Uri): Result<Unit> = withContext(dispatcher) {
        try {
            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val location = TreeUris.parse(DocumentsContract.getTreeDocumentId(treeUri))
            folderDao.insert(
                SelectedFolderEntity(
                    treeUri = treeUri.toString(),
                    displayName = location.displayName,
                    permissionState = FolderPermissionState.Granted,
                ),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Forgets the folder and releases the grant. Audio files are never touched. */
    suspend fun remove(id: Long) = withContext(dispatcher) {
        val folder = folderDao.byId(id) ?: return@withContext
        try {
            resolver.releasePersistableUriPermission(Uri.parse(folder.treeUri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // The grant may already be gone; the row is removed either way.
        }
        folderDao.deleteById(id)
    }

    /** Re-checks every saved folder: grant still held? folder still reachable? */
    suspend fun refreshStates() = withContext(dispatcher) {
        val held = resolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        for (folder in folderDao.all()) {
            val state = when {
                folder.treeUri !in held -> FolderPermissionState.Revoked
                !isReachable(Uri.parse(folder.treeUri)) -> FolderPermissionState.Unavailable
                else -> FolderPermissionState.Granted
            }
            if (state != folder.permissionState) folderDao.updatePermissionState(folder.id, state)
        }
    }

    private fun isReachable(treeUri: Uri): Boolean = try {
        val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    } catch (_: Exception) {
        false
    }

    private fun documentIdOrEmpty(treeUri: String): String = try {
        DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
    } catch (_: Exception) {
        ""
    }

    companion object {
        /** Route argument for a folder: the path with a trailing slash, or "/" for a whole volume. */
        fun routePath(prefix: String): String = if (prefix.isEmpty()) "/" else FolderNames.withTrailingSlash(prefix)

        fun prefixFromRoute(arg: String): String = if (arg == "/") "" else arg
    }
}
