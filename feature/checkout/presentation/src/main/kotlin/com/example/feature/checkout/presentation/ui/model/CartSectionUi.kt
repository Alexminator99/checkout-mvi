package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class CartSectionUi(val title: UiText, val lines: List<CartLineUi>, val isEmpty: Boolean)
