package com.example.feature.checkout.presentation

import com.example.core.ui.MoneyFormatter
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.projector.AddressSectionProjector
import com.example.feature.checkout.presentation.projector.CartSectionProjector
import com.example.feature.checkout.presentation.projector.CheckoutProjector
import com.example.feature.checkout.presentation.projector.PaymentSectionProjector
import com.example.feature.checkout.presentation.projector.PlaceOrderButtonProjector
import com.example.feature.checkout.presentation.projector.PromoSectionProjector
import com.example.feature.checkout.presentation.projector.ReadyCheckout
import com.example.feature.checkout.presentation.projector.ShippingSectionProjector
import com.example.feature.checkout.presentation.projector.SummarySectionProjector

/** Formats amounts the same way on every machine, whatever the JVM's default locale. */
internal val TestMoneyFormatter = MoneyFormatter { "€" + it.amount.toPlainString() }

internal fun checkoutProjector(): CheckoutProjector = CheckoutProjector(
    cart = CartSectionProjector(TestMoneyFormatter),
    address = AddressSectionProjector(),
    shipping = ShippingSectionProjector(TestMoneyFormatter),
    promo = PromoSectionProjector(TestMoneyFormatter),
    payment = PaymentSectionProjector(),
    summary = SummarySectionProjector(TestMoneyFormatter, CalculateOrderTotalsUseCase()),
    placeOrderButton = PlaceOrderButtonProjector(),
)

/** The input of a section projector, built from a state whose context has loaded. */
internal fun readyCheckout(state: CheckoutState = readyState()): ReadyCheckout =
    ReadyCheckout(state, (state.context as Loadable.Ready).value)
