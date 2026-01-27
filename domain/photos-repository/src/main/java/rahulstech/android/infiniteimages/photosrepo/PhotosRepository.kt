package rahulstech.android.infiniteimages.photosrepo

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rahulstech.android.data.unplash.UnsplashClient
import rahulstech.android.data.unplash.UnsplashService
import rahulstech.android.infiniteimages.database.PhotosDB
import rahulstech.android.infiniteimages.photosrepo.model.Photo
import rahulstech.android.infiniteimages.photosrepo.model.toPhoto
import rahulstech.android.infiniteimages.photosrepo.paging.PhotosRemoteMediator

class PhotosRepository(context: Context) {

    private val db: PhotosDB = PhotosDB.getInstance(context)

    private val service: UnsplashService = UnsplashClient().service

    private val repoData = RepositoryData(context)

    fun getPhotos(): Flow<PagingData<Photo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,

                // initially load photos of size initialLoadSize or its multiple
                // without this value it may trigger multiple requests on initial load
                initialLoadSize = 20,

                // default is pageSize, use smaller that pageSize
                prefetchDistance = 2,

                enablePlaceholders = false
            ),
            remoteMediator = PhotosRemoteMediator(db, service, repoData),
            pagingSourceFactory = { db.photoDao.getPhotos() }
        )
            .flow
            .map { pagingData -> pagingData.map { it.toPhoto() } }
    }

    suspend fun reset() = db.withTransaction {

        db.photoDao.deleteAllPhotos()

        db.photoRemoteKeyDao.deleteAllKeys()

        repoData.removeLastModifierMillis()
    }
}