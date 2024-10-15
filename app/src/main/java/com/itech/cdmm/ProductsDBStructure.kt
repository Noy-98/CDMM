package com.itech.cdmm

import java.io.Serializable

data class ProductsDBStructure(
    val p_id: String = "", // Primary key or unique identifier
    val product_name: String = "",
    val product_price: String = "",
    val product_size: String = "",
    val product_description: String = "",
    var product_image: String? = null
): Serializable
