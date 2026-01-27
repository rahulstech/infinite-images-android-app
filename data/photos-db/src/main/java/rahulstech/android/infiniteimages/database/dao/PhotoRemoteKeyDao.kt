package rahulstech.android.infiniteimages.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import rahulstech.android.infiniteimages.database.entity.PhotoRemoteKeyEntity

@Dao
interface PhotoRemoteKeyDao {

    // IMPORTANT: API may return photo with same id in multiple pages.
    // Since API returned id is the Primary Key in this table, therefore
    // UNIQUE constraint may fail during insert and no page will not load anymore.
    // To avoid this, SQL OnConflict REPLACE during INSERT strategy is used and it is necessary.
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleKeys(keys: List<PhotoRemoteKeyEntity>)

    @Query("SELECT * FROM `photo_remote_keys` WHERE `globalId` = :globalId")
    suspend fun getKeyById(globalId: String): PhotoRemoteKeyEntity?

    @Transaction
    @Query("DELETE FROM `photo_remote_keys`")
    suspend fun deleteAllKeys()
}