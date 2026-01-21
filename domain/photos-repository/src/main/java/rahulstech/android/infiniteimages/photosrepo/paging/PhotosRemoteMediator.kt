package rahulstech.android.infiniteimages.photosrepo.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import rahulstech.android.data.unplash.UnsplashService
import rahulstech.android.data.unplash.model.PhotoDto
import rahulstech.android.infiniteimages.database.PhotosDB
import rahulstech.android.infiniteimages.database.entity.PhotoEntity
import rahulstech.android.infiniteimages.database.entity.PhotoRemoteKeyEntity
import rahulstech.android.infiniteimages.photosrepo.PhotosRepositoryException
import rahulstech.android.infiniteimages.photosrepo.model.toPhotoEntity
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "PhotosRemoteMediator"

@OptIn(ExperimentalPagingApi::class)
class PhotosRemoteMediator(
    private val db: PhotosDB,
    private val service: UnsplashService
): RemoteMediator<Int, PhotoEntity>() {

    private val photosDao = db.photoDao

    private val photoKeysDao = db.photoRemoteKeyDao

    // load is responsible for loading pages from remote source add saving data into local source with remote keys.
    // when new Pager created REFRESH load is called. it's load()'s responsibility to decide weather the REFRESH call
    // actually requires a new remote source call. for example: cache expiry timestamp.
    // if REFRESH grants fetching remote source then first remove all local data with remote-keys
    override suspend fun load(loadType: LoadType, state: PagingState<Int, PhotoEntity>): MediatorResult {
        Log.i(TAG, "loadType=$loadType)")

        val pageIndex = when(loadType) {
            LoadType.REFRESH -> { 1 }

            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull() ?: return MediatorResult.Success(endOfPaginationReached = true)
                getNextPageIndex(lastItem.globalId) ?: return MediatorResult.Success(endOfPaginationReached = true)
            }

            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        try {
            // TODO: do i need to refetch from api for each loadType == REFRESH?

            val remotePhotos: List<PhotoDto> = loadRemotePage(pageIndex, 20)
            val photos: List<PhotoEntity> = remotePhotos.map { it.toPhotoEntity() }
            val endOfPaginationReached = remotePhotos.isEmpty()

            if (photos.isNotEmpty()) {
                savePageLocally(
                    photos,
                    prevPage = if (pageIndex == 1) null else pageIndex - 1,
                    nextPage = pageIndex + 1,
                    deleteOld = loadType == LoadType.REFRESH
                )
            }

            return MediatorResult.Success(endOfPaginationReached)
        }
        catch (cause: Throwable) {
            return MediatorResult.Error(cause)
        }
    }

    suspend fun getNextPageIndex(globalId: String): Int? {
        val remoteKey = db.photoRemoteKeyDao.getKeyById(globalId)
        return remoteKey?.nextPage
    }

    suspend fun loadRemotePage(page: Int, perPage: Int): List<PhotoDto> {
        try {
            val res = service.getPhotos(page, perPage)
            if (res.isSuccessful) {
                return res.body().orEmpty()
            } else {
                throw HttpException(res)
            }
        }
        catch (cause: IOException) {
            throw PhotosRepositoryException.NetworkException(cause)
        }
        catch (cause: HttpException) {
            throw PhotosRepositoryException.HttpException(cause.code(), cause.message(), cause)
        }
        catch (cause: Throwable) {
            throw PhotosRepositoryException.UnknownException(cause)
        }
    }

    suspend fun savePageLocally(photos: List<PhotoEntity>, prevPage: Int?, nextPage: Int?, deleteOld: Boolean = false) {
        Log.i(TAG,"saving ${photos.size} photos locally prevPage = $prevPage nextPage = $nextPage deleteOld = $deleteOld")
        db.withTransaction {
            if (deleteOld) {
                photosDao.deleteAllPhotos()
                photoKeysDao.deleteAllKeys()
            }

            photosDao.insertPhotos(photos)

            photoKeysDao.insertMultipleKeys(
                photos.map {
                    PhotoRemoteKeyEntity(
                        globalId = it.globalId,
                        prevPage = prevPage,
                        nextPage = nextPage
                    )
                }
            )
        }
    }
}