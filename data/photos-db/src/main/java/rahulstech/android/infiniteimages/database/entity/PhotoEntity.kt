package rahulstech.android.infiniteimages.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [
        Index(name = "index_photos_globalId", value = ["globalId"], unique = true)
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val globalId: String,
    val color: String,
    val blurHash: String,
    val description: String,
    val thumbnailUrl: String,
    val fullImageUrl: String,
    val downloadLink: String, // store the download_location link
)
