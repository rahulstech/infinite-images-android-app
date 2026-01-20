package rahulstech.android.infiniteimages

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.filterNotNull

private const val TAG = "MainActivity"

private const val THRESHOLD_LOAD_MORE = 5

private const val PAGE_SIZE = 20

data class MainState(
    val photos: List<PhotoDto> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
) {
    // while loading countPhotos considers current photos size and loading photos count
    // otherwise actual size of photos only. if i don't  use countPhoto then loading will
    // trigger twice near the bound
    val countPhotos: Int get() = if (isLoading) photos.size + PAGE_SIZE else photos.size
}

class MainViewModel: ViewModel() {

    private val client = UnsplashClient().api

    private val _state = MutableStateFlow(MainState())

    val state: StateFlow<MainState> = _state.asStateFlow()

    private var currentPageIndex = 0

    init {
        loadPage(1)
    }

    fun loadNextPage() {
        loadPage(currentPageIndex+1)
    }

    private fun loadPage(page: Int) {
        if (state.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                setLoading(true)
                val res = client.getPhotos(page, PAGE_SIZE)
                if (res.isSuccessful) {
                    updatePhotos(res.body())
                    currentPageIndex = page
                }
                else {
                    Log.e(TAG,"network error; ${res.errorBody()?.string()}")
                    setLoading(false)
                }
            }
            catch (cause: Throwable) {
                Log.e(TAG, "initial page loading error", cause)
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _state.value = _state.value.copy(isLoading = isLoading)
    }

    private fun updatePhotos(photos: List<PhotoDto>?) {
        val currentPhotos = _state.value.photos
        if (photos.isNullOrEmpty()) {
            _state.value = _state.value.copy(hasMore = false, isLoading = false)
        }
        else {
            _state.value = _state.value.copy(photos = currentPhotos + photos, isLoading = false)
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
    val state by viewmodel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .filterNotNull()
            .collect { lastVisibleItemIndex ->
                if (lastVisibleItemIndex >= state.countPhotos - THRESHOLD_LOAD_MORE) {
                    viewmodel.loadNextPage()
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(240.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = gridState
    ) {

        items(items = state.photos, key = { it.id }) { photo ->
            PhotoGridItem(photo)
        }

        if (state.isLoading) {
            items(count = 10) {
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

@Composable
fun ButtonLoadMore(onClick: ()-> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(onClick = onClick) {
            Text("Load More Photos")
        }
    }
}