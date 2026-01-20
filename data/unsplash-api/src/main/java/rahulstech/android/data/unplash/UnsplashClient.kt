package rahulstech.android.data.unplash

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import rahulstech.android.data.unsplash.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "UnsplashClient"

class UnsplashClient() {

    fun createGsonConverterFactory(): GsonConverterFactory =
        GsonConverterFactory.create()

    fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(requestHeadersInterceptor())
            .addNetworkInterceptor(debugResponseInterception())
        return builder.build()
    }

    private fun requestHeadersInterceptor(): Interceptor = Interceptor {
        val accessKey = BuildConfig.UNSPLASH_ACCESS_KEY
        val apiVersion = BuildConfig.UNSPLASH_API_VERSION
        val req = it.request()
        val newRequest = req.newBuilder()
            .addHeader("Authorization","Client-ID $accessKey")
            .addHeader("Accept-Version", apiVersion)
            .build()
        it.proceed(newRequest)
    }

    private fun debugResponseInterception(): Interceptor = Interceptor {
            val res  = it.proceed(it.request())
            if (BuildConfig.DEBUG) {
                val headers = res.headers()
                val limit = headers["X-Ratelimit-Limit"]
                val remaining = headers["X-Ratelimit-Remaining"]
                val reset = headers["X-Ratelimit-Reset"]
                Log.d(TAG,
                    "url=${it.request().url()}; status=${res.code()}; limit=$limit, remaining=$remaining, reset=$reset")
            }
            res
        }

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.unsplash.com")
        .addConverterFactory(createGsonConverterFactory())
        .client(createOkHttpClient())
        .build()

    val api: UnsplashService = retrofit.create(UnsplashService::class.java)
}

