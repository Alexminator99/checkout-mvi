package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface AddressStatusUi {
    data object None : AddressStatusUi
    data object Checking : AddressStatusUi
    data object Deliverable : AddressStatusUi
    data class Warning(val message: UiText) : AddressStatusUi
}
