package rahulstech.android.infiniteimages

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rahulstech.android.data.unplash.UnsplashClient
import rahulstech.android.data.unplash.model.PhotoDto
import rahulstech.android.infiniteimages.ui.theme.InfiniteImagesTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    val client = UnsplashClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfiniteImagesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        PhotosGrid(client)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotosGrid(client: UnsplashClient) {
    var photos by remember { mutableStateOf(emptyList<PhotoDto>()) }
    LaunchedEffect(client) {
        val res = withContext(Dispatchers.IO) {
            client.api.getPhotos()
        }

        if (res.isSuccessful) {
            photos = res.body() ?: emptyList()
            Log.d(TAG, "photos: $photos")
        }
        else {
            photos = emptyList()
            Log.e(TAG, res.errorBody()?.string() ?: "")
        }
    }

//    LazyColumn(modifier = Modifier.fillMaxSize()) {
//        items(items = photos, key = { it.id }) { photo ->
//            Text(photo.id, modifier = Modifier.height(36.dp).padding(16.dp))
//        }
//    }
}