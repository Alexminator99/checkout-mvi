package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class PaymentReducer @Inject constructor() : CheckoutSliceReducer<PaymentAction> {

    override fun reduce(state: CheckoutState, action: PaymentAction): CheckoutTransition = when (action) {
        is PaymentAction.Selected -> {
            val offered = state.contextOrNull?.paymentMethods?.any { it.id == action.paymentMethodId } == true
            if (!offered) state.only() else state.copy(selectedPaymentId = action.paymentMethodId).only()
        }
    }
}
