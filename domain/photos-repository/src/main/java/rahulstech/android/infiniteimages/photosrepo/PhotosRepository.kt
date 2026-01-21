package rahulstech.android.infiniteimages.photosrepo

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import rahulstech.android.data.unplash.UnsplashClient
import rahulstech.android.data.unplash.UnsplashService
import rahulstech.android.infiniteimages.database.PhotosDB
import rahulstech.android.infiniteimages.photosrepo.model.Photo
import rahulstech.android.infiniteimages.photosrepo.model.toPhoto
import rahulstech.android.infiniteimages.photosrepo.paging.PhotosRemoteMediator

class PhotosRepository(context: Context) {

    private val db: PhotosDB = PhotosDB.getInstance(context)

    private val service: UnsplashService = UnsplashClient().service

    fun getPhotos(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = PhotosRemoteMediator(db, service),
            pagingSourceFactory = { db.photoDao.getPhotos() }
        )
            .flow
            .map { pagingData -> pagingData.map { it.toPhoto() } }
    }
}