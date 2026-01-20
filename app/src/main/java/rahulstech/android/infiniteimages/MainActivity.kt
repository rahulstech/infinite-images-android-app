package rahulstech.android.infiniteimages

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rahulstech.android.data.unplash.UnsplashClient
import rahulstech.android.data.unplash.model.PhotoDto
import rahulstech.android.infiniteimages.ui.theme.InfiniteImagesTheme
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage

private const val TAG = "MainActivity"

class MainViewModel: ViewModel() {

    private val client = UnsplashClient().api

    private val _photos = MutableStateFlow<List<PhotoDto>>(emptyList())

    val photos: StateFlow<List<PhotoDto>> = _photos.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = client.getPhotos()
                if (res.isSuccessful) {
                    _photos.value = res.body() ?: emptyList()
                }
                else {
                    Log.e(TAG,"network error; ${res.errorBody()?.string() ?: ""}")
                }
            }
            catch (cause: Throwable) {
                Log.e(TAG, "initial page loading error", cause)
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    val viewmodel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfiniteImagesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Box(modifier = Modifier.widthIn(max = 800.dp)) {
                            PhotosGrid(viewmodel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotosGrid(viewmodel: MainViewModel) {
    val photos by viewmodel.photos.collectAsStateWithLifecycle()


    LazyVerticalGrid(
        columns = GridCells.Adaptive(240.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(items = photos, key = { it.id }) { photo ->
            PhotoGridItem(photo)
        }
    }
}

@Composable
fun PhotoGridItem(photo: PhotoDto) {
    val context = LocalContext.current
    val request = remember(photo.urls.thumb) {
        ImageRequest.Builder(context)
            .data(photo.urls.thumb)
            .placeholder(Color(photo.color.toColorInt()).toArgb().toDrawable())
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium),
        clipToBounds = true,
    )
}