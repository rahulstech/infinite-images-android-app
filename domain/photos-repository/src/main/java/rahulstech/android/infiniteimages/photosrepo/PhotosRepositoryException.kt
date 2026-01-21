package rahulstech.android.infiniteimages.photosrepo

sealed class PhotosRepositoryException(cause: Throwable?): Exception(cause)
{
    class NetworkException(cause: Throwable?): PhotosRepositoryException(cause)

    class HttpException(code: Int, message: String, cause: Throwable? = null): PhotosRepositoryException(cause)

    class UnknownException(cause: Throwable): PhotosRepositoryException(cause)
}