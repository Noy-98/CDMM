package com.itech.cdmm

import java.io.Serializable

data class CartDBStructure(
    val cart_id: String = "",
    val product_name: String = "",
    val product_price: String = "",
    val product_size: String = "",
    val product_description: String = "",
    val product_quantity: String = "1",
    val product_total_price: String = "",
    var product_image: String? = null
): Serializable
