package rahulstech.android.infiniteimages.photosrepo

import android.content.Context

private const val SP_NAME = "rahulstech.infiniteimages.shared_preference"

private const val KEY_LAST_MODIFIED_MILLIS = "last_modified"

class RepositoryData(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun rememberLastModified() {
        sp.edit().apply {
            putLong(KEY_LAST_MODIFIED_MILLIS, System.currentTimeMillis())
        }.commit()
    }

    fun getLastModifierMillis(): Long? {
        val lastModified = sp.getLong(KEY_LAST_MODIFIED_MILLIS, -1)
        return if (lastModified <= 0) null
        else lastModified
    }

    fun removeLastModifierMillis() {
        sp.edit().apply {
            remove(KEY_LAST_MODIFIED_MILLIS)
        }.commit()
    }
}