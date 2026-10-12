package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

/** Something the checkout asks the outside world for. */
internal sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Ready<out T>(val value: T) : Loadable<T>
    data class Failed(val error: CheckoutError) : Loadable<Nothing>
}
