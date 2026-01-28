package rahulstech.android.infiniteimages.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import rahulstech.android.infiniteimages.database.entity.PhotoEntity

@Dao
interface PhotoDao {

    @Transaction
    @Insert
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM `photos` ORDER BY `id` ASC") // it's better to use a consistent ordering
    fun getPhotos(): PagingSource<Int, PhotoEntity>

    @Transaction
    @Query("DELETE FROM `photos`")
    suspend fun deleteAllPhotos()
}