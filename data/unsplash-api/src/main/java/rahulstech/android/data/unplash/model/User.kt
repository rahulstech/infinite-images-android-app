package rahulstech.android.data.unplash.model

data class UserProfileImageDto(
    val small: String,
    val large: String,
)

data class UserDto(
    val id: String,
    val username: String,
    val name: String,
    val profile_image: UserProfileImageDto
)