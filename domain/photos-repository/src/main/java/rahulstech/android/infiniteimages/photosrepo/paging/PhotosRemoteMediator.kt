package rahulstech.android.infiniteimages.photosrepo.paging

import android.util.Log
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import rahulstech.android.data.unplash.UnsplashService
import rahulstech.android.infiniteimages.database.PhotosDB
import rahulstech.android.infiniteimages.database.entity.PhotoEntity
import rahulstech.android.infiniteimages.database.entity.PhotoRemoteKeyEntity
import rahulstech.android.infiniteimages.photosrepo.PhotosRepositoryException
import rahulstech.android.infiniteimages.photosrepo.RepositoryData
import rahulstech.android.infiniteimages.photosrepo.model.toPhotoEntity
import retrofit2.HttpException
import java.io.IOException

class PhotosRemoteMediator(
    private val db: PhotosDB,
    private val service: UnsplashService,
    private val repoData: RepositoryData
) : RemoteMediator<Int, PhotoEntity>() {

    companion object {
        private const val TAG = "PhotosRemoteMediator"

        private const val ITEMS_PER_PAGE = 20
    }

    private val photosDao = db.photoDao
    private val photoKeysDao = db.photoRemoteKeyDao

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PhotoEntity>
    ): MediatorResult {

        return when (loadType) {

            LoadType.REFRESH -> handleRefresh()

            LoadType.APPEND -> handleAppend(state)

            LoadType.PREPEND -> {
                // We never load backwards.
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }
    }


    private suspend fun handleRefresh(): MediatorResult {
        if (isFresh()) {
            Log.i(TAG, "db content is fresh")
            return MediatorResult.Success(false)
        }

        return loadPages(1, true)
    }

    private suspend fun handleAppend(state: PagingState<Int, PhotoEntity>): MediatorResult {
        val lastItem = state.lastItemOrNull()
            ?: return MediatorResult.Success(endOfPaginationReached = false)

        val remoteKey = photoKeysDao.getKeyById(lastItem.globalId)
            ?: return MediatorResult.Success(endOfPaginationReached = false)

        val nextPage = remoteKey.nextPage
            ?: return MediatorResult.Success(endOfPaginationReached = true)

        return loadPages(nextPage)
    }

    private suspend fun loadPages(
        startPage: Int,
        clearExisting: Boolean = false
    ): MediatorResult {
        return try {

            var endOfPaginationReached: Boolean

            val page = startPage
            val response = service.getPhotos(page, ITEMS_PER_PAGE)

            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val networkPhotos = response.body().orEmpty()
            endOfPaginationReached = networkPhotos.size < ITEMS_PER_PAGE

            db.withTransaction {

                if (clearExisting) {
                    photoKeysDao.deleteAllKeys()
                    photosDao.deleteAllPhotos()
                }

                Log.i(
                    TAG,
                    "add ${networkPhotos.size} photos to db and endOfPaginationReached = $endOfPaginationReached"
                )

                if (networkPhotos.isEmpty()) {
                    return@withTransaction
                }

                val entities = networkPhotos.map { it.toPhotoEntity() }

                photosDao.insertPhotos(entities)

                // NOTE: Unsplash API returns a Link header which contains the first, prev, next and last page links
                // however is not necessary to parse this header to construct the PhotoRemoteKeyEntity entries.
                val keys = entities.map {
                    PhotoRemoteKeyEntity(
                        globalId = it.globalId,
                        prevPage = if (page == 1) null else page - 1,
                        nextPage = if (endOfPaginationReached) null else page + 1
                    )
                }

                photoKeysDao.insertMultipleKeys(keys)

                repoData.rememberLastModified()
            }

            MediatorResult.Success(endOfPaginationReached)

        } catch (cause: Throwable) {
            Log.e(TAG, "error while loading in RemoteMediator", cause)
            when(cause) {
                is IOException -> {
                    MediatorResult.Error(PhotosRepositoryException.NetworkException(cause))
                }
                is HttpException -> {
                    MediatorResult.Error(
                        PhotosRepositoryException.HttpException(cause.code(), cause.message(), cause
                        )
                    )
                }
                else -> {
                    MediatorResult.Error(PhotosRepositoryException.UnknownException(cause))
                }
            }
        }
    }

    // it is a fancy freshness check to avoid loading same photos withing 24 hours
    // in real case I may need to consider the content expiry or similar header or
    // other meta data to ensure freshness and reload the content if necessary.
    private fun isFresh(): Boolean =
        repoData.getLastModifierMillis()?.let { lastModifiedMillis ->
            System.currentTimeMillis() - lastModifiedMillis < 86400000 // 24 hours
        } ?: false
}