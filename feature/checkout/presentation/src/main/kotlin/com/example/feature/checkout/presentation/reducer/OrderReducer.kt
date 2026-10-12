package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import javax.inject.Inject

internal class OrderReducer @Inject constructor() : CheckoutSliceReducer<OrderAction> {

    override fun reduce(state: CheckoutState, action: OrderAction): CheckoutTransition = when (action) {
        OrderAction.PlaceClicked -> {
            val request = state.orderRequestOrNull()
            if (request == null) state.only()
            else state.copy(submission = Submission.Submitting).withEffects(CheckoutEffect.PlaceOrder(request))
        }

        is OrderAction.Placed ->
            state.copy(submission = Submission.Idle).withEvent(CheckoutEvent.NavigateToConfirmation(action.order.id))

        is OrderAction.Failed -> {
            val failed = state.copy(submission = Submission.Failed(action.error)).only()
            // Prices moved under the customer: re-quote so the new total is shown before paying again.
            if (action.error == CheckoutError.PriceChanged) failed.then { it.requestQuotes() } else failed
        }

        OrderAction.ErrorDismissed -> state.copy(submission = Submission.Idle).only()
    }
}
