package com.example.feature.checkout.projector

import com.example.core.mvi.Projector
import com.example.core.ui.UiText
import com.example.feature.checkout.R
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.Submission
import com.example.feature.checkout.contract.orderRequestOrNull
import com.example.feature.checkout.ui.model.AddressSectionUi
import com.example.feature.checkout.ui.model.CartSectionUi
import com.example.feature.checkout.ui.model.CheckoutUiState
import com.example.feature.checkout.ui.model.PaymentSectionUi
import com.example.feature.checkout.ui.model.PlaceOrderButtonUi
import com.example.feature.checkout.ui.model.PromoSectionUi
import com.example.feature.checkout.ui.model.ShippingSectionUi
import com.example.feature.checkout.ui.model.SummaryUi
import javax.inject.Inject

/** Screen projector: decides the top-level shape, then composes section projectors. */
internal class CheckoutProjector @Inject constructor(
    private val cart: SectionProjector<CartSectionUi>,
    private val address: SectionProjector<AddressSectionUi>,
    private val shipping: SectionProjector<ShippingSectionUi>,
    private val promo: SectionProjector<PromoSectionUi>,
    private val payment: SectionProjector<PaymentSectionUi>,
    private val summary: SectionProjector<SummaryUi>,
) : Projector<CheckoutState, CheckoutUiState> {

    override fun project(state: CheckoutState): CheckoutUiState = when (val context = state.context) {
        Loadable.Idle, Loadable.Loading -> CheckoutUiState.Loading
        is Loadable.Failed -> CheckoutUiState.Error(context.error.toUiText(), canRetry = true)
        is Loadable.Ready -> {
            val ready = ReadyCheckout(state, context.value)
            val summaryUi = summary.project(ready)
            CheckoutUiState.Content(
                offlineBanner = !context.value.isOnline,
                cart = cart.project(ready),
                address = address.project(ready),
                shipping = shipping.project(ready),
                promo = promo.project(ready),
                payment = payment.project(ready),
                summary = summaryUi,
                placeOrder = PlaceOrderButtonUi(
                    label = UiText.res(R.string.checkout_pay_amount, summaryUi.total),
                    enabled = state.orderRequestOrNull() != null,    // same rule the reducer guards with
                    loading = state.submission == Submission.Submitting,
                ),
                blockingError = (state.submission as? Submission.Failed)?.error?.toUiText(),
            )
        }
    }
}
