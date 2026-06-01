package com.kawaii.meowbah.ui.screens.stats

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Channel Stats") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val htmlData = """
                <html>
                <body style="margin:0;padding:0;display:flex;flex-direction:column;justify-content:center;align-items:center;background-color:transparent;gap:16px;">
                    <iframe height="80px" width="300px" frameborder="0" src="https://livecounts.io/embed/youtube-live-subscriber-counter/UCNytjdD5-KZInxjVeWV_qQw" style="border: 0; width:300px; height:80px;"></iframe>
                    <iframe height="80px" width="300px" frameborder="0" src="https://livecounts.io/embed/tiktok-live-follower-counter/meowbahx" style="border: 0; width:300px; height:80px;"></iframe>
                </body>
                </html>
            """.trimIndent()

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        setBackgroundColor(0) // Transparent
                        loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }
}
