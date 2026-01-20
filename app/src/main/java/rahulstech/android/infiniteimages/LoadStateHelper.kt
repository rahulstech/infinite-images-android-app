package rahulstech.android.infiniteimages

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

val CombinedLoadStates.refreshing: Boolean  get() = refresh is LoadState.Loading

val CombinedLoadStates.appending: Boolean get() = append is LoadState.Loading