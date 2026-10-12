package com.example.feature.checkout.presentation.contract

sealed interface LifecycleAction : CheckoutAction {

    /** Sent once by the ViewModel when the screen starts. Not an intent: the UI cannot restart the data stream. */
    data object Started : LifecycleAction

    data object RetryLoadClicked : LifecycleAction, CheckoutIntent
}
