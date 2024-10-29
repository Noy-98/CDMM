package com.itech.cdmm

import java.io.Serializable

data class CartDBStructure(
    val cart_id: String = "",
    val product_name: String = "",
    val product_price: String = "",
    var product_size: String = "",
    val product_description: String = "",
    var product_quantity: String = "1",
    var product_total_price: String = "",
    var product_image: String? = null
): Serializable
