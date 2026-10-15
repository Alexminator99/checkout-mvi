package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

sealed interface LifecycleAction : CheckoutAction {

    /** Sent once by the ViewModel when the screen starts. Not an intent: the UI cannot restart the data stream. */
    data object Started : LifecycleAction

    data object ResetDemoClicked : LifecycleAction, CheckoutIntent
    data object DemoReset : LifecycleAction
    data class DemoResetFailed(val error: CheckoutError) : LifecycleAction

    data object RetryLoadClicked : LifecycleAction, CheckoutIntent
}
