package com.kawaii.meowbah.ui.screens.merch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R // For placeholder
import com.kawaii.meowbah.data.model.MerchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchScreen(
    onMerchClick: (String) -> Unit,
    merchViewModel: MerchViewModel = viewModel()
) {
    val merchItems by merchViewModel.merchItems.collectAsState()
    val cartItems by merchViewModel.cartItems.collectAsState()
    val searchQuery by merchViewModel.searchQuery.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutWebView by remember { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (!searchActive) {
                        Text("Merch Store") 
                    } else {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { merchViewModel.onSearchQueryChange(it) },
                            onSearch = { searchActive = false },
                            active = searchActive,
                            onActiveChange = { searchActive = it },
                            placeholder = { Text("Search Merch") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            // Search suggestions could go here
                        }
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showCartSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (cartItems.isNotEmpty()) {
                                        Badge {
                                            Text(cartItems.sumOf { it.quantity }.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp) // Set to 0 for edge-to-edge
    ) { paddingValues ->
        if (merchItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text("No merchandise available yet. Stay tuned!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // You can adjust the number of columns
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(merchItems) { item ->
                    MerchGridItem(
                        merchItem = item,
                        onItemClick = { onMerchClick(item.id) },
                        onAddToCartClick = { merchViewModel.addToCart(item) }
                    )
                }
            }
        }

        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = sheetState
            ) {
                CartSheet(
                    viewModel = merchViewModel,
                    onCheckoutClick = {
                        showCartSheet = false
                        showCheckoutWebView = true
                    }
                )
            }
        }

        if (showCheckoutWebView) {
            BasicAlertDialog(
                onDismissRequest = { showCheckoutWebView = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        TopAppBar(
                            title = { Text("Checkout") },
                            navigationIcon = {
                                IconButton(onClick = { showCheckoutWebView = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                        CheckoutWebView(
                            cartItems = cartItems
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchGridItem(
    merchItem: MerchItem,
    onItemClick: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(merchItem.imageUrl ?: merchItem.imageResId) // Use imageUrl if available
                        .crossfade(true)
                        .placeholder(R.drawable.ic_placeholder) // Generic placeholder
                        .error(R.drawable.ic_launcher_background) // Error placeholder
                        .build(),
                    contentDescription = merchItem.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // Makes the image square
                    contentScale = ContentScale.Crop
                )
                
                FilledTonalIconButton(
                    onClick = onAddToCartClick,
                    modifier = Modifier.padding(4.dp).size(32.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Add to Cart",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = merchItem.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = merchItem.price,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
