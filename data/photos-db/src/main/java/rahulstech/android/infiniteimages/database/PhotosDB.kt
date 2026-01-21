package rahulstech.android.infiniteimages.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import rahulstech.android.infiniteimages.database.dao.PhotoDao
import rahulstech.android.infiniteimages.database.dao.PhotoRemoteKeyDao
import rahulstech.android.infiniteimages.database.entity.PhotoEntity
import rahulstech.android.infiniteimages.database.entity.PhotoRemoteKeyEntity

@Database(
    entities = [PhotoEntity::class, PhotoRemoteKeyEntity::class],
    version = PhotosDB.DB_VERSION,
)
abstract class PhotosDB: RoomDatabase() {

    companion object {
        const val DB_VERSION = 1

        const val DB_NAME = "photos.db3"

        private val LOCK = Any()

        private lateinit var instance: PhotosDB

        fun getInstance(context: Context): PhotosDB = synchronized(LOCK) {
            if (!::instance.isInitialized) {
                instance = Room.databaseBuilder(context, PhotosDB::class.java, DB_NAME)
                    .build()
            }
            instance
        }
    }

    abstract val photoDao: PhotoDao

    abstract val photoRemoteKeyDao: PhotoRemoteKeyDao
}