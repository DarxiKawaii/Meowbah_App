package com.kawaii.meowbah.data.model

import androidx.annotation.DrawableRes

/**
 * Represents a single merchandise item.
 *
 * @param id A unique identifier for the merch item.
 * @param name The name of the merchandise.
 * @param description A short description of the merchandise.
 * @param price The price of the merchandise (e.g., "$19.99").
 * @param imageResId The local drawable resource ID for the merch image.
 * @param storeUrl An optional URL to the product page on an external store.
 */
data class MerchItem(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    @DrawableRes val imageResId: Int? = null,
    val imageUrl: String? = null,
    val storeUrl: String? = null,
    val variantId: String? = null
)
