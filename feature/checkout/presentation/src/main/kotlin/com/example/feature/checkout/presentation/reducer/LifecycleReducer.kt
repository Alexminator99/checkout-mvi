package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import javax.inject.Inject

internal class LifecycleReducer @Inject constructor() : CheckoutSliceReducer<LifecycleAction> {

    override fun reduce(state: CheckoutState, action: LifecycleAction): CheckoutTransition = when (action) {
        LifecycleAction.Started -> state.withEffects(CheckoutEffect.ObserveContext)
        LifecycleAction.RetryLoadClicked ->
            state.copy(context = Loadable.Loading).withEffects(CheckoutEffect.ObserveContext)
    }
}
