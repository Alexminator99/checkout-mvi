package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.isCheckoutLocked
import javax.inject.Inject

internal class LifecycleReducer @Inject constructor() : CheckoutSliceReducer<LifecycleAction> {

    override fun reduce(state: CheckoutState, action: LifecycleAction): CheckoutTransition = when (action) {
        LifecycleAction.Started -> state.withEffects(CheckoutEffect.ObserveContext)
        LifecycleAction.ResetDemoClicked ->
            if (state.isUpdatingCart || state.isCheckoutLocked) state.only()
            else CheckoutState(checkoutId = state.checkoutId, isResettingDemo = true).withEffects(CheckoutEffect.ResetDemo)
        LifecycleAction.DemoReset -> CheckoutState(checkoutId = state.checkoutId).withEffects(CheckoutEffect.ObserveContext)
        is LifecycleAction.DemoResetFailed ->
            state.copy(isResettingDemo = false, context = Loadable.Failed(action.error)).only()
        LifecycleAction.RetryLoadClicked ->
            state.copy(context = Loadable.Loading).withEffects(CheckoutEffect.ObserveContext)
    }
}
