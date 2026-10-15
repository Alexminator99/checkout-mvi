package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.isCheckoutLocked
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import javax.inject.Inject

/**
 * Turns state into what the screen draws: decides the overall shape, then lets each section projector
 * describe its section. A reducer answers "what happened?"; a projector answers "what does it look like?".
 */
internal class CheckoutProjector @Inject constructor(
    private val cart: CartSectionProjector,
    private val address: AddressSectionProjector,
    private val shipping: ShippingSectionProjector,
    private val promo: PromoSectionProjector,
    private val payment: PaymentSectionProjector,
    private val summary: SummarySectionProjector,
    private val placeOrderButton: PlaceOrderButtonProjector,
) {

    fun project(state: CheckoutState): CheckoutUiState = when (val context = state.context) {
        Loadable.Idle, Loadable.Loading -> CheckoutUiState.Loading
        is Loadable.Failed -> CheckoutUiState.Error(context.error.toUiText())
        is Loadable.Ready -> {
            val ready = ReadyCheckout(state, context.value)
            val summaryUi = summary.project(ready)
            CheckoutUiState.Content(
                isOffline = !context.value.isOnline,
                cart = cart.project(ready),
                address = address.project(ready),
                shipping = shipping.project(ready),
                promo = promo.project(ready),
                payment = payment.project(ready),
                summary = summaryUi,
                placeOrder = placeOrderButton.project(state, summaryUi.total),
                blockingError = when (state.priceRefresh) {
                    Loadable.Loading -> UiText.res(R.string.checkout_wait_prices)
                    is Loadable.Failed -> UiText.res(R.string.checkout_retry_prices)
                    else -> (state.submission as? Submission.Failed)?.error?.toUiText()
                },
                errorDismissEnabled = state.priceRefresh !is Loadable.Loading,
                retryPriceRefresh = state.priceRefresh is Loadable.Failed,
                demoResetEnabled = !state.isCheckoutLocked && !state.isUpdatingCart,
            )
        }
    }
}
