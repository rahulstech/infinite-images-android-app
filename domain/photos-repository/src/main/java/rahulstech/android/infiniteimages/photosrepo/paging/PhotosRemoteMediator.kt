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

private const val  TAG = "PhotosRemoteMediator"

class PhotosRemoteMediator(
    private val db: PhotosDB,
    private val service: UnsplashService,
    private val repoData: RepositoryData
) : RemoteMediator<Int, PhotoEntity>() {

    private val photosDao = db.photoDao
    private val photoKeysDao = db.photoRemoteKeyDao

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PhotoEntity>
    ): MediatorResult {

        val page = when (loadType) {

            LoadType.REFRESH -> {

                if (isFresh()) {
                    Log.i(TAG, "db content is fresh")
                    return MediatorResult.Success(false)
                }

                state.anchorPosition?.let { position ->
                    state.closestItemToPosition(position)?.let { item ->
                        photoKeysDao.getKeyById(item.globalId)
                    }
                }?.nextPage?.minus(1) ?: 1

            }

            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                    ?: return MediatorResult.Success(endOfPaginationReached = false)

                val remoteKey = photoKeysDao.getKeyById(lastItem.globalId)
                    ?: return MediatorResult.Success(endOfPaginationReached = false)

                remoteKey.nextPage
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }

            LoadType.PREPEND -> {
                // We never load backwards.
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        Log.i(TAG, "loadType $loadType pageIndex $page")

        return try {
            val response = service.getPhotos(page,20)

            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val networkPhotos = response.body().orEmpty()
            val endOfPaginationReached = networkPhotos.isEmpty()

            db.withTransaction {

                if (loadType == LoadType.REFRESH) {
                    photoKeysDao.deleteAllKeys()
                    photosDao.deleteAllPhotos()
                }

                Log.i(TAG, "add ${networkPhotos.size} photos to db and endOfPaginationReached = $endOfPaginationReached")

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