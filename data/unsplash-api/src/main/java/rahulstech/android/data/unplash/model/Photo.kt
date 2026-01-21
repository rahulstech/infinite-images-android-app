package rahulstech.android.data.unplash.model

data class PhotoUrlDto(
    val full: String,
    val thumb: String,
)

data class PhotoLinkDto(
    val self: String,
    val download: String,
)

data class PhotoDto(
    val id: String,
    val color: String,
    val blur_hash: String,
    val description: String?,
    val alt_description: String?,
    val urls: PhotoUrlDto,
    val links: PhotoLinkDto,
)
