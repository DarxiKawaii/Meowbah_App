package com.kawaii.meowbah.ui.screens.merch

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.kawaii.meowbah.data.model.CartItem

@Composable
fun CheckoutWebView(
    cartItems: List<CartItem>
) {
    val checkoutUrl = buildCheckoutUrl(cartItems)

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true // Required for many modern checkout pages
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                loadUrl(checkoutUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildCheckoutUrl(cartItems: List<CartItem>): String {
    val baseUrl = "https://meowbah-shop.fourthwall.com/cart/checkout?products="
    val productParams = cartItems.mapNotNull { item ->
        item.merchItem.variantId?.let { "$it:${item.quantity}" }
    }.joinToString(",")
    
    return "$baseUrl$productParams&currency=USD"
}
