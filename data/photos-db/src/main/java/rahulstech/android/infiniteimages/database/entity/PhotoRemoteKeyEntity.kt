package rahulstech.android.infiniteimages.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * this table stores the per item previous and next page.
 * it helps RemoteMediator to decide what is previous or next page
 * based on photo id. therefore photo id from api is stored not the
 * room id.
 *
 * Note: Paging does not allow dynamic per page size. therefore a photo
 * will be always in the same page until the per page size value is unchanged.
 * if per page size is changed then all rows in this table also deleted.
 * so, there is no change of inconsistency.
 */
@Entity(tableName = "photo_remote_keys")
data class PhotoRemoteKeyEntity(
    @PrimaryKey
    val globalId: String, // id given by api
    val prevPage: Int?, // for first page prevPage is null
    val nextPage: Int?, // for last page nextPage is null
)
