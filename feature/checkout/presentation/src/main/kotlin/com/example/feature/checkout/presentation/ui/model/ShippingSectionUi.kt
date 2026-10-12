package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface ShippingSectionUi {
    data object Hidden : ShippingSectionUi
    data object Loading : ShippingSectionUi
    data class Error(val message: UiText) : ShippingSectionUi
    data class Options(val rows: List<SelectableRowUi>) : ShippingSectionUi
}
