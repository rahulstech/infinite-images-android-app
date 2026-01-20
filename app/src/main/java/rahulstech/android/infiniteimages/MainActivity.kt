package rahulstech.android.infiniteimages

import android.os.Bundle
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import rahulstech.android.data.unplash.UnsplashClient
import rahulstech.android.data.unplash.model.PhotoDto
import rahulstech.android.infiniteimages.ui.theme.InfiniteImagesTheme
import androidx.core.graphics.toColorInt
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import rahulstech.android.data.unplash.paging.PhotosPagingSource

private const val TAG = "MainActivity"

private const val THRESHOLD_LOAD_MORE = 5

private const val PAGE_SIZE = 20

class MainViewModel: ViewModel() {

    private val service = UnsplashClient().service

    val photos = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = THRESHOLD_LOAD_MORE
        ),
        pagingSourceFactory = { PhotosPagingSource(service) }
    )
        .flow
        .cachedIn(viewModelScope)
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
            else -> {}
        }

        if (loadState.refreshing || loadState.appending) {
            items(count = PAGE_SIZE) {
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
