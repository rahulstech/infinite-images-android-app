package rahulstech.android.infiniteimages

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import rahulstech.android.infiniteimages.ui.theme.InfiniteImagesTheme
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import rahulstech.android.infiniteimages.photosrepo.PhotosRepository
import rahulstech.android.infiniteimages.photosrepo.model.Photo

private const val TAG = "MainActivity"

class MainViewModel(app: Application): AndroidViewModel(app) {

    private val repo: PhotosRepository = PhotosRepository(app)

    val photos = repo.getPhotos().cachedIn(viewModelScope)
}

class MainActivity : ComponentActivity() {

    val viewmodel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfiniteImagesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
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
    val photos = viewmodel.photos.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()


    LazyVerticalGrid(
        columns = GridCells.Adaptive(240.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = gridState
    ) {

        val loadState = photos.loadState

        when(loadState.refresh) {
            is LoadState.NotLoading -> {
                items(count = photos.itemCount, key = { index -> photos[index]?.id ?: index }) { index ->
                    photos[index]?.let {  PhotoGridItem(it) }
                }
            }
            is LoadState.Error -> {
                Log.e(TAG,"refresh error", (loadState.refresh as LoadState.Error).error)
            }
            else -> {}
        }

        if (loadState.append is LoadState.Error) {
            Log.e(TAG,"refresh error", (loadState.append as LoadState.Error).error)
        }

        if (loadState.refreshing || loadState.appending) {
            items(count = if (loadState.refreshing) 30 else 5) {
                PhotoGridItemShimmer()
            }
        }
    }
}

@Composable
fun PhotoGridItemShimmer() {
    Box(modifier = Modifier.size(240.dp)
        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
        .clip(MaterialTheme.shapes.medium)
    )
}

@Composable
fun PhotoGridItem(photo: Photo) {
    val context = LocalContext.current
    val request = remember(photo.thumbnail) {
        ImageRequest.Builder(context)
            .data(photo.thumbnail)
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
