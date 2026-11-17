package com.sonara.app.app

import android.content.Context
import com.sonara.app.core.permissions.AudioPermissionChecker
import com.sonara.app.data.local.database.SonaraDatabase
import com.sonara.app.data.local.preferences.SettingsRepository
import com.sonara.app.data.local.preferences.sonaraDataStore

/**
 * Manual dependency container, created once by [SonaraApplication].
 * Everything is lazy so nothing opens the database or reads preferences during process start.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SonaraDatabase by lazy { SonaraDatabase.create(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.sonaraDataStore) }
    val audioPermissionChecker: AudioPermissionChecker by lazy { AudioPermissionChecker(appContext) }
}
