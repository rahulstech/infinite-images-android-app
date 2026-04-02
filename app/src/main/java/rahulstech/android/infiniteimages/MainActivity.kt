package rahulstech.android.infiniteimages

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

private val ITEM_WIDTH = 150.dp


private const val TYPE_SHIMMER = 1

private const val TYPE_PHOTO = 2

private const val TYPE_ERROR = 3


class MainViewModel(app: Application): AndroidViewModel(app) {

    private val repo: PhotosRepository = PhotosRepository(app)

    val photos = repo.getPhotos().cachedIn(viewModelScope)

    fun reset(onComplete: ()-> Unit) {
        viewModelScope.launch {
            repo.reset()
            onComplete()
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.app_name))
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(modifier = Modifier
                            .widthIn(max = 800.dp)
                            .padding(16.dp)) {
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

    val refreshState by rememberUpdatedState(photos.loadState.refresh)

    PullToRefreshBox(
        isRefreshing = refreshState is LoadState.Loading,
        onRefresh = {
            viewModel.reset {
                photos.refresh()
            }
        },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        when (refreshState) {

            is LoadState.Error -> {
                RefreshError(
                    onRetry = {
                        viewModel.reset { photos.refresh() }
                    }
                )
            }

            else -> {
                PhotosGrid(photos)
            }
        }
    }
}

@Composable
fun PhotosGrid(photos: LazyPagingItems<Photo> ) {
    val gridState = rememberLazyGridState()
    val refreshState by rememberUpdatedState(photos.loadState.refresh)
    val appendState by rememberUpdatedState(photos.loadState.append)

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = GridCells.Adaptive(ITEM_WIDTH),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        when(refreshState) {
            is LoadState.Loading -> {
                items(
                    count = 6,
                    key = { index -> "placeholder_$index" },
                    contentType = { _ -> TYPE_SHIMMER }
                ) {
                    PhotoGridItemShimmer()
                }
            }

            is LoadState.NotLoading -> {
                items(
                    count = photos.itemCount,
                    key = { index -> photos[index]?.id ?: "shimmer_$index"  },
                    contentType = { index ->
                        if (photos[index] == null) TYPE_SHIMMER else TYPE_PHOTO
                    },
                ) { index ->
                    val item = photos[index]
                    if (null != item) {
                        PhotoGridItem(item)
                    }
                    else {
                        PhotoGridItemShimmer()
                    }
                }
            }

            else -> {}
        }


        when(appendState) {
            is LoadState.Error ->  {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    AppendError(onRetry = { photos.retry() })
                }
            }

            else -> {}
        }
    }
}

@Composable
fun PhotoGridItemShimmer() {
    Box(modifier = Modifier
        .size(ITEM_WIDTH)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
    )
}

@Composable
fun PhotoGridItem(photo: Photo) {
    val context = LocalContext.current
    val request = remember(photo.thumbnail) {
        val fallback =  Color(photo.color.toColorInt()).toArgb().toDrawable()
        ImageRequest.Builder(context)
            .data(photo.thumbnail)
            .crossfade(true)
            .placeholder(fallback)
            .fallback(fallback)
            .error(fallback)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium),
        clipToBounds = true,
    )
}

@Composable
fun AppendError(onRetry: ()-> Unit) {
    ErrorItem(
        message = "Fail to load next photos",
        onRetry = onRetry
    )
}

@Composable
fun RefreshError(onRetry: ()-> Unit) {
    ErrorItem(
        message = "Fail to load photos",
        onRetry = onRetry
    )
}

@Composable
fun ErrorItem(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                shape = MaterialTheme.shapes.small
            ).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(16.dp))

        FilledTonalButton(
            onClick = onRetry,
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                "Retry",
            )
        }
    }
}

@Preview( showBackground = true )
@Composable
fun ErrorItemPreview() {
    InfiniteImagesTheme {
        ErrorItem(
            message = "Error Message",
            onRetry = {}
        )
    }
}
