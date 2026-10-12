package com.example.feature.checkout.presentation.projector

import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.CheckoutState

/** The input of a section projector. It exists only once the context has loaded, so sections need no null checks. */
internal data class ReadyCheckout(val state: CheckoutState, val context: CheckoutContext)
