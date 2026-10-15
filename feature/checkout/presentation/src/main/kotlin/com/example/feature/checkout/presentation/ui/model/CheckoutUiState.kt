package com.example.feature.checkout.presentation.ui.model

import com.example.core.ui.UiText

sealed interface CheckoutUiState {

    data object Loading : CheckoutUiState

    data class Error(val message: UiText) : CheckoutUiState

    data class Content(
        val isOffline: Boolean,
        val cart: CartSectionUi,
        val address: AddressSectionUi,
        val shipping: ShippingSectionUi,
        val promo: PromoSectionUi,
        val payment: PaymentSectionUi,
        val summary: SummaryUi,
        val placeOrder: PlaceOrderButtonUi,
        val blockingError: UiText?,
        val errorDismissEnabled: Boolean = true,
        val retryPriceRefresh: Boolean = false,
        val demoResetEnabled: Boolean = true,
    ) : CheckoutUiState
}
