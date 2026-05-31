package com.kawaii.meowbah.ui.screens.merch

import android.app.Application
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
        try {
            val jsonString = getApplication<Application>().assets.open("all.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val productsArray = jsonObject.getJSONArray("products")
            val items = mutableListOf<MerchItem>()

            for (i in 0 until productsArray.length()) {
                val product = productsArray.getJSONObject(i)
                val variantsArray = product.getJSONArray("variants")
                val firstVariantId = if (variantsArray.length() > 0) {
                    variantsArray.getJSONObject(0).getString("id")
                } else null

                items.add(
                    MerchItem(
                        id = product.getString("id"),
                        name = product.getString("title"),
                        description = "Available: ${product.getBoolean("available")}\nURL: ${product.getString("url")}",
                        price = "$" + product.getString("price"),
                        imageUrl = product.getString("image"),
                        storeUrl = "https://meowbah-shop.fourthwall.com" + product.getString("url"),
                        variantId = firstVariantId
                    )
                )
            }
            _merchItems.value = items
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback or empty list
        }
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
