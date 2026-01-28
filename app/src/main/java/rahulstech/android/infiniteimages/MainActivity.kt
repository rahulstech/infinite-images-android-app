package rahulstech.android.infiniteimages

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import rahulstech.android.infiniteimages.ui.theme.InfiniteImagesTheme
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import rahulstech.android.infiniteimages.photosrepo.PhotosRepository
import rahulstech.android.infiniteimages.photosrepo.model.Photo

private const val TAG = "MainActivity"

class MainViewModel(app: Application): AndroidViewModel(app) {

    private val repo: PhotosRepository = PhotosRepository(app)

    val photos = repo.getPhotos().cachedIn(viewModelScope)

    fun refresh() {
        viewModelScope.launch {
            repo.reset()
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
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(modifier = Modifier.widthIn(max = 800.dp)) {
                            PhotosScreen(viewmodel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotosScreen(viewModel: MainViewModel) {
    val photos = viewModel.photos.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (photos.loadState.refresh) {
            is LoadState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.width(12.dp))

                    Text("Loading initial images", style = MaterialTheme.typography.bodyLarge)
                }
            }
            is LoadState.Error -> PhotosGridError(onRetry = { photos.retry() })
            is LoadState.NotLoading -> PhotosGrid(photos)
        }
    }
}

@Composable
fun PhotosGrid(photos: LazyPagingItems<Photo>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(240.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = photos.itemCount,
            key = { index -> photos[index]?.id ?: index }
        ) { index ->
            photos[index]?.let { PhotoGridItem(it) }
        }

        // Footer load state (append)
        when (photos.loadState.append) {
            is LoadState.Loading -> {
                items(count = 2) {
                    PhotoGridItemShimmer()
                }
            }
            is LoadState.Error -> {
                item {
                    PhotoGridItemError(onRetry = { photos.retry() })
                }
            }
            else -> Unit
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

@Composable
fun PhotoGridItemError(onRetry: ()-> Unit) {
    Box(
        modifier = Modifier.size(240.dp)
            .clip(MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onRetry) {
            Text("Retry", fontSize = 24.sp)
        }
    }
}

@Composable
fun PhotosGridError(onRetry: ()-> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onRetry, modifier = Modifier.width(96.dp).height(48.dp)) {
            Text("Retry", fontSize = 24.sp)
        }
    }
}
