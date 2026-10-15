package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.contextOrNull
import com.example.feature.checkout.presentation.contract.isAddressDeliverable
import com.example.feature.checkout.presentation.contract.isPlacingOrder
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import com.example.feature.checkout.presentation.contract.selectedShippingOption
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import javax.inject.Inject

internal class PlaceOrderButtonProjector @Inject constructor() {
    fun project(state: CheckoutState, total: String): PlaceOrderButtonUi {
        val enabled = state.orderRequestOrNull() != null
        return PlaceOrderButtonUi(
            label = UiText.res(R.string.checkout_pay_amount, total),
            enabled = enabled,
            loading = state.isPlacingOrder,
            disabledReason = if (enabled || state.isPlacingOrder) null else reason(state),
        )
    }

    private fun reason(state: CheckoutState): UiText = UiText.res(when {
        state.isUpdatingCart -> R.string.checkout_wait_cart
        state.priceRefresh is Loadable.Loading -> R.string.checkout_wait_prices
        state.priceRefresh is Loadable.Failed -> R.string.checkout_retry_prices
        state.contextOrNull?.isOnline == false -> R.string.checkout_wait_online
        state.contextOrNull?.cart?.isEmpty != false -> R.string.checkout_wait_items
        state.address.validation is Loadable.Loading -> R.string.checkout_wait_address
        !state.isAddressDeliverable -> R.string.checkout_choose_address
        state.shipping.quotes is Loadable.Loading -> R.string.checkout_wait_shipping
        state.selectedShippingOption == null -> R.string.checkout_choose_shipping
        state.promo.status is PromoStatus.Redeeming -> R.string.checkout_wait_promo
        state.submission == Submission.Completed -> R.string.confirmation_title
        state.submission is Submission.Failed -> R.string.checkout_review_error
        else -> R.string.checkout_choose_payment
    })
}
