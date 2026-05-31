package com.kawaii.meowbah.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.ArrayList
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    onVideoClick: (String) -> Unit,
    viewModel: VideosViewModel
) {
    val videosState: List<CachedVideoInfo> by viewModel.videos.collectAsState()
    val filters by viewModel.generatedFilters.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val errorMessage: String? by viewModel.error.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val youtubeChannelUrl = "https://www.youtube.com/@Meowbahx"

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results: ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                searchQuery = results[0]
                searchActive = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!searchActive && videosState.isEmpty() && !isLoading) {
            viewModel.fetchVideos()
        }
    }

    LaunchedEffect(videosState) {
        if (videosState.isNotEmpty() && filters.size <= 1) {
            viewModel.generateFilters()
        }
    }

    val filteredVideos = videosState.filter { video ->
        val matchesQuery = video.title.contains(searchQuery, ignoreCase = true) || 
                          (video.description?.contains(searchQuery, ignoreCase = true) == true)
        val matchesFilter = selectedFilter == "All" || 
                           video.title.contains(selectedFilter, ignoreCase = true) ||
                           (video.description?.contains(selectedFilter, ignoreCase = true) == true)
        matchesQuery && matchesFilter
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (!searchActive) {
                        Text("Kawaii Videos")
                    } else {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { searchActive = false },
                            active = searchActive,
                            onActiveChange = { searchActive = it },
                            placeholder = { Text("Search Kawaii Videos") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                    }
                                    try {
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                                    }
                                }) {
                                    Icon(Icons.Filled.Mic, contentDescription = "Voice search")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            val suggestions = videosState.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) 
                            }
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(suggestions) { video ->
                                    ListItem(
                                        headlineContent = { Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.clickable {
                                            searchQuery = video.title
                                            searchActive = false
                                            onVideoClick(video.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (!searchActive) {
                        FilledTonalIconButton(
                            onClick = { searchActive = true },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showClearCacheDialog = true },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                text = { Text("Clear Cache") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { viewModel.selectFilter(filter) },
                                    label = { Text(filter) }
                                )
                            }
                        }
                    }
                    items(filteredVideos) { video ->
                        VideoListItem(video = video, onVideoClick = { clickedVideo ->
                            onVideoClick(clickedVideo.id)
                        })
                    }
                    if (filteredVideos.isNotEmpty()) {
                        item {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeChannelUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text("Show More on YouTube")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Video Cache") },
            text = { Text("This will remove all cached videos. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VideoListItem(video: CachedVideoInfo, onVideoClick: (CachedVideoInfo) -> Unit) {
    val publishedDateFormatted: String = video.publishedAt?.let { pubAtNonNull ->
        try {
            OffsetDateTime.parse(pubAtNonNull)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()))
        } catch (e: Exception) {
            pubAtNonNull
        }
    } ?: "Date unavailable"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onVideoClick(video) }
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl ?: R.drawable.ic_placeholder)
                    .crossfade(true)
                    .error(R.drawable.ic_placeholder)
                    .placeholder(R.drawable.ic_placeholder)
                    .build(),
                contentDescription = "Video thumbnail for ${video.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = publishedDateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
