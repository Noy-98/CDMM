package com.itech.cdmm

import java.io.Serializable

data class NotificationDBStructure(
    val notification_id: String = "", // Primary key or unique identifier
    val title: String = "",
    val subject: String = "",
    val from: String = "",
    val to: String = "",
    val message: String = ""
): Serializable
