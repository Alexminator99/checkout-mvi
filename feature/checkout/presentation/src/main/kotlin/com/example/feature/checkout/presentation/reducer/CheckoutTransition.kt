package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.Reducer
import com.example.core.mvi.Transition
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState

internal typealias CheckoutTransition = Transition<CheckoutState, CheckoutEffect, CheckoutEvent>

internal typealias CheckoutSliceReducer<A> = Reducer<CheckoutState, A, CheckoutEffect, CheckoutEvent>
