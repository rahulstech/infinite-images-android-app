package rahulstech.android.data.unplash

import rahulstech.android.data.unplash.model.PhotoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashService {

    @GET("/photos")
    suspend fun getPhotos(@Query("page") page: Int = 1,
                          @Query("per_page") perPage: Int = 10
                          ): Response<List<PhotoDto>>
}