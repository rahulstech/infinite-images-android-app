package rahulstech.android.infiniteimages.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import rahulstech.android.infiniteimages.database.entity.PhotoRemoteKeyEntity

@Dao
interface PhotoRemoteKeyDao {

    @Transaction
    @Insert
    suspend fun insertMultipleKeys(keys: List<PhotoRemoteKeyEntity>)

    @Query("SELECT * FROM `photo_remote_keys` WHERE `globalId` = :globalId")
    suspend fun getKeyById(globalId: String): PhotoRemoteKeyEntity?

    @Transaction
    @Query("DELETE FROM `photo_remote_keys`")
    suspend fun deleteAllKeys()
}