package com.kawaii.meowbah.ui.screens.merch

import android.app.Application
import android.text.Html
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kawaii.meowbah.data.model.CartItem
import com.kawaii.meowbah.data.model.MerchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject

class MerchViewModel(application: Application) : AndroidViewModel(application) {

    private val _merchItems = MutableStateFlow<List<MerchItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val merchItems: StateFlow<List<MerchItem>> = _merchItems.combine(_searchQuery) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    val totalPrice: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { (it.merchItem.price.removePrefix("$").toDoubleOrNull() ?: 0.0) * it.quantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        loadMerchItemsFromAssets()
    }

    private fun loadMerchItemsFromAssets() {
        val jsonFiles = listOf("1.json", "2.json", "3.json", "4.json")
        val allLoadedItemsMap = mutableMapOf<String, MerchItem>()

        for (fileName in jsonFiles) {
            try {
                val jsonString = getApplication<Application>().assets.open(fileName).bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                val productsArray = jsonObject.getJSONArray("products")
                
                android.util.Log.d("MerchViewModel", "Loading $fileName: ${productsArray.length()} products found")

                for (i in 0 until productsArray.length()) {
                    try {
                        val product = productsArray.optJSONObject(i) ?: continue
                        val id = product.optString("id")
                        if (id.isEmpty()) continue
                        
                        // Only add if not already present to avoid duplicates
                        if (!allLoadedItemsMap.containsKey(id)) {
                            val variantsArray = product.optJSONArray("variants")
                            val firstVariantId = if (variantsArray != null && variantsArray.length() > 0) {
                                variantsArray.getJSONObject(0).optString("id")
                            } else null

                            val rawTitle = product.optString("title", "Unknown Item")
                            val decodedTitle = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString()
                            
                            val item = MerchItem(
                                id = id,
                                name = decodedTitle,
                                description = "", // Description removed per request
                                price = "$" + product.optString("price", "0.00"),
                                imageUrl = product.optString("image", ""),
                                storeUrl = "https://meowbah-shop.fourthwall.com" + product.optString("url", ""),
                                variantId = firstVariantId,
                                isAvailable = product.optBoolean("available", true)
                            )
                            allLoadedItemsMap[id] = item
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MerchViewModel", "Error parsing product at index $i in $fileName", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MerchViewModel", "Error opening or parsing file $fileName", e)
            }
        }
        android.util.Log.d("MerchViewModel", "Total unique items loaded: ${allLoadedItemsMap.size}")
        _merchItems.value = allLoadedItemsMap.values.toList()
    }

    fun getMerchItemById(id: String): MerchItem? {
        return _merchItems.value.find { it.id == id }
    }

    fun addToCart(merchItem: MerchItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst { it.merchItem.id == merchItem.id }

        if (existingItemIndex != -1) {
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentItems.add(CartItem(merchItem, 1))
        }
        _cartItems.value = currentItems
    }

    fun removeFromCart(merchItem: MerchItem) {
        val currentItems = _cartItems.value.toMutableList()
        currentItems.removeAll { it.merchItem.id == merchItem.id }
        _cartItems.value = currentItems
    }

    fun updateQuantity(merchItem: MerchItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(merchItem)
            return
        }
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst { it.merchItem.id == merchItem.id }

        if (existingItemIndex != -1) {
            currentItems[existingItemIndex] = currentItems[existingItemIndex].copy(quantity = newQuantity)
            _cartItems.value = currentItems
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
