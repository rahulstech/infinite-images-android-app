package rahulstech.android.data.unplash.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import rahulstech.android.data.unplash.UnsplashService
import rahulstech.android.data.unplash.model.PhotoDto

private const val TAG = "PhotosPagingSource"

private const val PER_PAGE = 20

class PhotosPagingSource(private val service: UnsplashService): PagingSource<Int, PhotoDto>() {
    override fun getRefreshKey(state: PagingState<Int, PhotoDto>): Int? {
        // calculate the current page based on the current state
        return state.anchorPosition?.let { anchor ->
            val closest = state.closestPageToPosition(anchor)
            closest?.prevKey?.plus(1)
                ?: closest?.nextKey?.minus(1)
        }
    }


    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PhotoDto> {
        val page = params.key ?: 1

        // loadSize is not always per page item size, paging may request more items for a key
        // but source may supply less items for the keys. so it is better to use source specific
        // per page size rather than loadSize as per page size
        // for example: paging requested key = 1 and loadSize = 60
        // so i request page=1&per_page=60 and returns items 0 to 59
        // now suppose next time paging requested key = 2 and loadSize = 20
        // the api request like page=2&per_page=20 this time returns 20 to 39
        // because it assumes that
        // page 1: 0-19
        // page 2: 20-39
        // so i get duplicate items.
//        val loadSize = params.loadSize

        return try {
            val res = service.getPhotos(page, PER_PAGE)
            if (res.isSuccessful) {
                val photos = res.body().orEmpty()
                return LoadResult.Page(
                    data = photos,
                    prevKey = if (page == 1) null else page-1,
                    nextKey = if (photos.isEmpty()) null else page+1
                )
            }
            else {
                // http error
                val error = Exception(res.errorBody()?.string())
                Log.e(TAG, "http error", error)
                return LoadResult.Error(error)
            }
        }
        catch (cause: Throwable) {
            // network error
            Log.e(TAG, "network error", cause)
            LoadResult.Error(cause)
        }
    }
}

