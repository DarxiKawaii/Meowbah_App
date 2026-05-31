package com.kawaii.meowbah.ui.screens.merch

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Share // Added for Share icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kawaii.meowbah.R // For placeholder
import com.kawaii.meowbah.data.model.MerchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchDetailScreen(
    merchId: String?,
    merchViewModel: MerchViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var merchItem by remember { mutableStateOf<MerchItem?>(null) }
    
    // We'll need a way to show a snackbar if we add to cart from here
    // But this screen is usually in a Dialog or Sheet.
    // Let's just add the button for now.

    LaunchedEffect(merchId) {
        if (merchId != null) {
            merchItem = merchViewModel.getMerchItemById(merchId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (merchItem == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (merchId == null) {
                    Text("Error: Merch ID not provided.")
                } else {
                    Text("Loading merch details or item not found...")
                }
            }
        } else {
            merchItem?.let { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Merch Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row {
                            FilledTonalIconButton(onClick = {
                                val shareText = if (item.storeUrl != null) {
                                    "Check out this merch: ${item.name}. Available here: ${item.storeUrl}"
                                } else {
                                    "Check out this merch: ${item.name}"
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Merch Via"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share Merch")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalIconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl ?: item.imageResId)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_launcher_background)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(bottom = 16.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Price: ${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            merchViewModel.addToCart(item)
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                       .fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cart")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    item.storeUrl?.let {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                context.startActivity(intent)
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                           .fillMaxWidth(0.8f)
                        ) {
                            Text("View in Store")
                        }
                    }
                }
            }
        }
    }
}
