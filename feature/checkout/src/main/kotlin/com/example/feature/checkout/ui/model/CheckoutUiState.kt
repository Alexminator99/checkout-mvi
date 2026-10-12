package com.example.feature.checkout.ui.model

import com.example.core.ui.UiText

/** Exactly what Compose draws. No domain types leak past this point. */
sealed interface CheckoutUiState {

    data object Loading : CheckoutUiState

    data class Error(val message: UiText, val canRetry: Boolean) : CheckoutUiState

    data class Content(
        val offlineBanner: Boolean,
        val cart: CartSectionUi,
        val address: AddressSectionUi,
        val shipping: ShippingSectionUi,
        val promo: PromoSectionUi,
        val payment: PaymentSectionUi,
        val summary: SummaryUi,
        val placeOrder: PlaceOrderButtonUi,
        val blockingError: UiText?,
    ) : CheckoutUiState
}

data class CartSectionUi(val title: UiText, val lines: List<CartLineUi>, val isEmpty: Boolean)

data class CartLineUi(
    val sku: String,
    val name: String,
    val quantity: Int,
    val lineTotal: String,
    val canDecrement: Boolean,
    val canIncrement: Boolean,
)

data class SelectableRowUi(
    val id: String,
    val title: UiText,
    val subtitle: UiText?,
    val trailing: UiText?,
    val selected: Boolean,
)

data class AddressSectionUi(val rows: List<SelectableRowUi>, val status: AddressStatusUi)

sealed interface AddressStatusUi {
    data object None : AddressStatusUi
    data object Checking : AddressStatusUi
    data object Deliverable : AddressStatusUi
    data class Warning(val message: UiText) : AddressStatusUi
}

sealed interface ShippingSectionUi {
    data object Hidden : ShippingSectionUi
    data object Loading : ShippingSectionUi
    data class Error(val message: UiText) : ShippingSectionUi
    data class Options(val rows: List<SelectableRowUi>) : ShippingSectionUi
}

data class PromoSectionUi(
    val input: String,
    val inputEnabled: Boolean,
    val applyEnabled: Boolean,
    val isRedeeming: Boolean,
    val applied: UiText?,
    val error: UiText?,
)

data class PaymentSectionUi(val rows: List<SelectableRowUi>)

data class SummaryUi(val rows: List<SummaryRowUi>, val total: String)

data class SummaryRowUi(val label: UiText, val value: UiText)

data class PlaceOrderButtonUi(val label: UiText, val enabled: Boolean, val loading: Boolean)
