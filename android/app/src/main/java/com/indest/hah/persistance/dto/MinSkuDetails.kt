package com.indest.hah.persistance.dto

data class MinSkuDetails(
    val sku: String,
    val title: String,
    val price: String,
    val canPurchase: Boolean,
    val isPurchased: Boolean
)