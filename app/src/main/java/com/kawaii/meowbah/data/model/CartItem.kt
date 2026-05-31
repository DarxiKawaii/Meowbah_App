package com.kawaii.meowbah.data.model

/**
 * Represents an item in the shopping cart.
 *
 * @param merchItem The merchandise item.
 * @param quantity The number of items in the cart.
 */
data class CartItem(
    val merchItem: MerchItem,
    val quantity: Int
)
