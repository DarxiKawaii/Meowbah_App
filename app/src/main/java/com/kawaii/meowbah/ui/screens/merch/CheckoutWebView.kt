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
                settings.javaScriptEnabled = true
                loadUrl(checkoutUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildCheckoutUrl(cartItems: List<CartItem>): String {
    val baseUrl = "https://meowbah-shop.fourthwall.com/cart/"
    val cartData = cartItems.joinToString(",") { item ->
        "${item.merchItem.variantId ?: ""}:${item.quantity}"
    }
    return baseUrl + cartData
}
