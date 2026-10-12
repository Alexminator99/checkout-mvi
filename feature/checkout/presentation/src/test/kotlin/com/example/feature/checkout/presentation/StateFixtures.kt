package com.example.feature.checkout.presentation

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice

internal const val CHECKOUT_ID = "checkout-1"

/** A state that meets every precondition for placing an order. */
internal fun readyState(context: CheckoutContext = checkoutContext()): CheckoutState = CheckoutState(
    checkoutId = CHECKOUT_ID,
    context = Loadable.Ready(context),
    address = AddressSlice(selectedId = HOME.id, validation = Loadable.Ready(AddressValidation.Deliverable)),
    shipping = ShippingSlice(quotes = Loadable.Ready(listOf(STANDARD, PICKUP, EXPRESS)), selectedId = STANDARD.id),
    selectedPaymentId = VISA.id,
)
