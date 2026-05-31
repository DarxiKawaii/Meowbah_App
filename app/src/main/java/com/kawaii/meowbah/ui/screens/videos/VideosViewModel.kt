package com.kawaii.meowbah.ui.screens.videos

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.CachedVideoInfo
import com.kawaii.meowbah.data.db.AppDatabase
import com.kawaii.meowbah.data.db.VideoEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.kawaii.meowbah.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val videoDao = AppDatabase.getInstance(application).videoDao()

    private val _videos = MutableStateFlow<List<CachedVideoInfo>>(emptyList())
    val videos: StateFlow<List<CachedVideoInfo>> = _videos

    private val _generatedFilters = MutableStateFlow<List<String>>(listOf("All"))
    val generatedFilters: StateFlow<List<String>> = _generatedFilters

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    companion object {
        private const val TAG = "VideosViewModel"
        private const val RSS_FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=UCNytjdD5-KZInxjVeWV_qQw"
        private const val NS_ATOM = "http://www.w3.org/2005/Atom"
        private const val NS_YT = "http://www.youtube.com/xml/schemas/2015"
        private const val NS_MEDIA = "http://search.yahoo.com/mrss/"
    }

    private data class RssFeedVideoItem(
        val id: String,
        val title: String,
        val publishedAt: String?,
        val description: String?,
        val thumbnailUrl: String?
    )

    init {
        // Observe database
        viewModelScope.launch {
            videoDao.getAllVideos().collectLatest { entities ->
                _videos.value = entities.map {
                    CachedVideoInfo(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        thumbnailUrl = it.thumbnailUrl,
                        publishedAt = it.publishedAt
                    )
                }
            }
        }
    }

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = _videos.value.isEmpty() // Only show full loading if cache is empty
            _error.value = null
            try {
                val feedItems = fetchAndParseRssFeed()
                if (feedItems.isNotEmpty()) {
                    val entities = feedItems.map {
                        VideoEntity(
                            id = it.id,
                            title = it.title,
                            description = it.description,
                            thumbnailUrl = it.thumbnailUrl,
                            publishedAt = it.publishedAt
                        )
                    }
                    videoDao.insertVideos(entities)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching videos", e)
                if (_videos.value.isEmpty()) {
                    _error.value = "Failed to load videos. Please check your connection."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun generateFilters() {
        viewModelScope.launch {
            val currentVideos = _videos.value
            if (currentVideos.isEmpty()) return@launch

            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val prompt = "Analyze the following video titles and descriptions. " +
                    "Generate 5-6 concise and distinct category filters (1-2 words each) that group these videos well. " +
                    "Return ONLY the category names separated by commas. Do not include 'All'.\n\n" +
                    currentVideos.take(15).joinToString("\n") { "Title: ${it.title}, Description: ${it.description}" }

                val response = generativeModel.generateContent(prompt)
                val aiFilters = response.text?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                
                if (aiFilters.isNotEmpty()) {
                    _generatedFilters.value = listOf("All") + aiFilters
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini filter generation failed", e)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            videoDao.clearAllVideos()
            _generatedFilters.value = listOf("All")
            _selectedFilter.value = "All"
        }
    }

    private suspend fun fetchAndParseRssFeed(): List<RssFeedVideoItem> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<RssFeedVideoItem>()
        var connection: HttpURLConnection? = null
        var reader: InputStreamReader? = null

        try {
            val url = URL(RSS_FEED_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server responded with ${connection.responseCode}")
            }

            reader = InputStreamReader(connection.inputStream, Charsets.UTF_8)
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(reader)

            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, NS_ATOM, "feed")

            while (parser.next() != XmlPullParser.END_TAG || parser.name != "feed") {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                if (parser.namespace == NS_ATOM && parser.name == "entry") {
                    var currentVideoId: String? = null
                    var currentTitle: String? = null
                    var currentPublishedAt: String? = null
                    var currentDescription: String? = null
                    var currentThumbnailUrl: String? = null

                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "entry") {
                        if (parser.eventType != XmlPullParser.START_TAG) continue
                        when (parser.namespace) {
                            NS_ATOM -> when (parser.name) {
                                "title" -> currentTitle = readText(parser)
                                "published" -> currentPublishedAt = readText(parser)
                                else -> skip(parser)
                            }
                            NS_YT -> when (parser.name) {
                                "videoId" -> currentVideoId = readText(parser)
                                else -> skip(parser)
                            }
                            NS_MEDIA -> when (parser.name) {
                                "group" -> {
                                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "group") {
                                        if (parser.eventType != XmlPullParser.START_TAG) continue
                                        if (parser.namespace == NS_MEDIA) {
                                            when (parser.name) {
                                                "description" -> currentDescription = readText(parser)
                                                "thumbnail" -> {
                                                    if (currentThumbnailUrl == null) {
                                                         currentThumbnailUrl = parser.getAttributeValue(null, "url")
                                                    }
                                                    skip(parser)
                                                }
                                                else -> skip(parser)
                                            }
                                        } else skip(parser)
                                    }
                                }
                                else -> skip(parser)
                            }
                            else -> skip(parser)
                        }
                    }
                    if (currentVideoId != null && currentTitle != null) {
                        entries.add(RssFeedVideoItem(currentVideoId, currentTitle, currentPublishedAt, currentDescription, currentThumbnailUrl))
                    }
                } else skip(parser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error", e)
            throw e
        } finally {
            reader?.close()
            connection?.disconnect()
        }
        entries
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result.trim()
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) throw IllegalStateException()
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
