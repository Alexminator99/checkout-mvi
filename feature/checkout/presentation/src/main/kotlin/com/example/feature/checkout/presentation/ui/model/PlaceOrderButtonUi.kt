package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

data class PlaceOrderButtonUi(val label: UiText, val enabled: Boolean, val loading: Boolean, val disabledReason: UiText? = null)
