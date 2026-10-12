package com.example.feature.checkout.contract

import com.example.domain.error.DomainError
import com.example.domain.model.AddressValidation
import com.example.domain.model.CheckoutContext
import com.example.domain.model.Order
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption

/**
 * Everything the reducer can receive, grouped by slice (routing = Single Responsibility).
 *
 * Two audiences, enforced by types (Interface Segregation):
 *  - [CheckoutIntent]: what the UI is allowed to send. The ViewModel only accepts these.
 *  - Everything else: results reported by effect handlers. The UI cannot fake them.
 */
sealed interface CheckoutAction

/** User intents. A subset of actions, marked by a second sealed supertype. */
sealed interface CheckoutIntent : CheckoutAction

sealed interface LifecycleAction : CheckoutAction {
    data object Started : LifecycleAction, CheckoutIntent
    data object RetryLoadClicked : LifecycleAction, CheckoutIntent
}

sealed interface ContextAction : CheckoutAction {
    data class Updated(val context: CheckoutContext) : ContextAction
    data class Failed(val error: DomainError) : ContextAction
}

sealed interface CartAction : CheckoutAction {
    data class QuantityChanged(val sku: String, val quantity: Int) : CartAction, CheckoutIntent
    data class UpdateFailed(val error: DomainError) : CartAction
}

sealed interface AddressAction : CheckoutAction {
    data class Selected(val addressId: String) : AddressAction, CheckoutIntent
    data class Validated(val addressId: String, val result: AddressValidation) : AddressAction
    data class ValidationFailed(val addressId: String, val error: DomainError) : AddressAction
}

sealed interface ShippingAction : CheckoutAction {
    data class Selected(val optionId: String) : ShippingAction, CheckoutIntent
    data object RetryClicked : ShippingAction, CheckoutIntent
    data class QuotesLoaded(val addressId: String, val options: List<ShippingOption>) : ShippingAction
    data class QuotesFailed(val addressId: String, val error: DomainError) : ShippingAction
}

sealed interface PromoAction : CheckoutAction {
    data class InputChanged(val text: String) : PromoAction, CheckoutIntent
    data object ApplyClicked : PromoAction, CheckoutIntent
    data object RemoveClicked : PromoAction, CheckoutIntent
    data class Redeemed(val promo: Promo) : PromoAction
    data class Rejected(val error: DomainError) : PromoAction
}

sealed interface PaymentAction : CheckoutAction {
    data class Selected(val paymentMethodId: String) : PaymentAction, CheckoutIntent
}

sealed interface OrderAction : CheckoutAction {
    data object PlaceClicked : OrderAction, CheckoutIntent
    data object ErrorDismissed : OrderAction, CheckoutIntent
    data class Placed(val order: Order) : OrderAction
    data class Failed(val error: DomainError) : OrderAction
}
