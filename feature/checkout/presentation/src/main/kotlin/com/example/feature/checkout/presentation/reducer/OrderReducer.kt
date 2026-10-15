package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.contextOrNull
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import javax.inject.Inject

internal class OrderReducer @Inject constructor(private val context: ContextReducer) : CheckoutSliceReducer<OrderAction> {
    override fun reduce(state: CheckoutState, action: OrderAction): CheckoutTransition = when (action) {
        OrderAction.PlaceClicked -> {
            val request = state.orderRequestOrNull()
            if (request == null) state.only()
            else state.copy(submission = Submission.Submitting).withEffects(CheckoutEffect.PlaceOrder(request))
        }
        is OrderAction.Placed -> state.copy(submission = Submission.Completed, deferredContext = null)
            .withEvent(CheckoutEvent.NavigateToConfirmation(action.order.id))
        is OrderAction.Failed -> {
            val failed = state.copy(submission = Submission.Failed(action.error), deferredContext = null)
            val reconciled = state.deferredContext?.let { context.reduce(failed, ContextAction.Updated(it)) } ?: failed.only()
            if (action.error == CheckoutError.PriceChanged) reconciled.then { it.refreshPrices() } else reconciled
        }
        is OrderAction.PricesRefreshed -> {
            val settled = state.copy(priceRefresh = Loadable.Idle)
            val current = settled.contextOrNull
            if (current == null) settled.only()
            else context.reduce(settled, ContextAction.Updated(current.copy(cart = action.cart)))
        }
        is OrderAction.PriceRefreshFailed -> state.copy(priceRefresh = Loadable.Failed(action.error)).only()
        OrderAction.ErrorDismissed -> when (state.priceRefresh) {
            is Loadable.Failed -> state.refreshPrices()
            Loadable.Loading -> state.only()
            else -> state.copy(submission = Submission.Idle).only()
        }
    }

    private fun CheckoutState.refreshPrices(): CheckoutTransition =
        copy(priceRefresh = Loadable.Loading).withEffects(CheckoutEffect.RefreshCart)
}
