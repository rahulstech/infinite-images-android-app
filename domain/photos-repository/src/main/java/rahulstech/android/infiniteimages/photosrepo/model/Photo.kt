package rahulstech.android.infiniteimages.photosrepo.model

import rahulstech.android.data.unplash.model.PhotoDto
import rahulstech.android.infiniteimages.database.entity.PhotoEntity

data class Photo(
    val id: Long,
    val description: String = "",
    val color: String,
    val blurHash: String,
    val thumbnail: String,
    val fullImage: String,
    val downloadLink: String,
)

internal fun PhotoDto.toPhotoEntity(): PhotoEntity =
    PhotoEntity(
        id = 0,
        globalId = id,
        description = description ?: alt_description ?: "",
        color = color,
        blurHash =  blur_hash,
        thumbnailUrl = urls.thumb,
        fullImageUrl = urls.full,
        downloadLink = links.download_location,
    )

internal fun PhotoEntity.toPhoto(): Photo =
    Photo(
        id = id,
        description = description,
        color = color,
        blurHash = blurHash,
        thumbnail = thumbnailUrl,
        fullImage = fullImageUrl,
        downloadLink = downloadLink
    )