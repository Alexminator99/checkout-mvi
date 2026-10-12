package com.example.feature.checkout.presentation.ui.model

data class CartLineUi(
    val sku: String,
    val name: String,
    val quantity: Int,
    val lineTotal: String,
    val canDecrement: Boolean,
    val decrementRemovesLine: Boolean,
    val canIncrement: Boolean,
)
