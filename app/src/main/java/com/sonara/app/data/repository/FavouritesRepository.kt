package com.sonara.app.data.repository

import com.sonara.app.data.local.dao.FavouriteDao
import com.sonara.app.data.local.database.FavouriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Favourite tracks, in the existing `favourites` table (no schema change). Tracks are referenced by MediaStore id. */
class FavouritesRepository(private val dao: FavouriteDao) {
    val trackIds: Flow<Set<Long>> = dao.observeTrackIds().map { it.toHashSet() }

    suspend fun setFavourite(trackId: Long, favourite: Boolean, now: Long = System.currentTimeMillis()) {
        if (favourite) dao.insert(FavouriteEntity(trackId = trackId, createdAt = now)) else dao.deleteByTrackId(trackId)
    }
}
