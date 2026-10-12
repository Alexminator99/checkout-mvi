package com.example.feature.checkout.presentation.contract

/**
 * The subset of [CheckoutAction]s the UI may send. Effect results are not intents, so the type
 * system keeps the UI from faking them.
 */
sealed interface CheckoutIntent : CheckoutAction
