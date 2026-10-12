package com.example.feature.checkout.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.domain.error.DomainError
import com.example.domain.model.CheckoutContext
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.AddressSlice
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.ContextAction
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PaymentAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.PromoSlice
import com.example.feature.checkout.contract.PromoStatus
import com.example.feature.checkout.contract.ShippingAction
import com.example.feature.checkout.contract.ShippingSlice
import com.example.feature.checkout.contract.Submission
import com.example.feature.checkout.contract.availableShippingOptions
import com.example.feature.checkout.contract.contextOrNull
import com.example.feature.checkout.contract.orderRequestOrNull
import javax.inject.Inject

// One reducer per slice: each has a single reason to change, and each is
// tested on its own. All of them are pure and dependency-free.

internal class LifecycleReducer @Inject constructor() : CheckoutSliceReducer<LifecycleAction> {
    override fun reduce(state: CheckoutState, action: LifecycleAction): CheckoutTransition = when (action) {
        // Idempotent: a second Started (e.g. re-subscription) restarts the keyed subscription.
        LifecycleAction.Started -> state.withEffects(CheckoutEffect.ObserveContext)
        LifecycleAction.RetryLoadClicked ->
            state.copy(context = Loadable.Loading).withEffects(CheckoutEffect.ObserveContext)
    }
}

internal class ContextReducer @Inject constructor() : CheckoutSliceReducer<ContextAction> {

    override fun reduce(state: CheckoutState, action: ContextAction): CheckoutTransition = when (action) {
        is ContextAction.Updated -> onUpdated(state, action.context)
        is ContextAction.Failed ->
            // Keep showing what we have if a refresh fails; only block the screen on first load.
            if (state.contextOrNull != null) state.withEvent(CheckoutEvent.ShowTransientError(action.error))
            else state.copy(context = Loadable.Failed(action.error)).only()
    }

    private fun onUpdated(state: CheckoutState, context: CheckoutContext): CheckoutTransition {
        val previous = state.contextOrNull
        val addressBefore = state.address.selectedId
        val cartChanged = previous != null && previous.cart != context.cart

        return state.copy(context = Loadable.Ready(context)).only()
            .then { it.reconcileAddress(context) }
            .then { it.reconcilePayment(context) }
            .then { s ->
                if (!cartChanged) return@then s.only()
                // If the address just changed, its re-quote is already in flight.
                val requote = if (s.address.selectedId == addressBefore) s.requestQuotes() else s.only()
                requote.then { it.revalidatePromo() }
            }
            .then { it.reconcileShippingSelection() }   // the express flag may have flipped
    }

    private fun CheckoutState.reconcileAddress(context: CheckoutContext): CheckoutTransition {
        if (context.addresses.any { it.id == address.selectedId }) return only()
        val fallback = context.addresses.firstOrNull { it.isDefault } ?: context.addresses.firstOrNull()
        return if (fallback != null) selectAddress(fallback.id)
        else copy(address = AddressSlice(), shipping = ShippingSlice()).only()
    }

    private fun CheckoutState.reconcilePayment(context: CheckoutContext): CheckoutTransition {
        if (context.paymentMethods.any { it.id == selectedPaymentId }) return only()
        val fallback = context.paymentMethods.firstOrNull { it.isDefault } ?: context.paymentMethods.firstOrNull()
        return copy(selectedPaymentId = fallback?.id).only()
    }
}

internal class CartReducer @Inject constructor() : CheckoutSliceReducer<CartAction> {
    override fun reduce(state: CheckoutState, action: CartAction): CheckoutTransition = when (action) {
        is CartAction.QuantityChanged -> {
            val line = state.contextOrNull?.cart?.lines?.firstOrNull { it.sku == action.sku }
            // No optimistic update: the cart stream is the source of truth and will re-emit.
            if (line == null || line.quantity == action.quantity) state.only()
            else state.withEffects(CheckoutEffect.UpdateQuantity(action.sku, action.quantity))
        }
        is CartAction.UpdateFailed -> state.withEvent(CheckoutEvent.ShowTransientError(action.error))
    }
}

internal class AddressReducer @Inject constructor() : CheckoutSliceReducer<AddressAction> {
    override fun reduce(state: CheckoutState, action: AddressAction): CheckoutTransition = when (action) {
        is AddressAction.Selected -> {
            val known = state.contextOrNull?.addresses?.any { it.id == action.addressId } == true
            val alreadyValid = action.addressId == state.address.selectedId && state.address.validation !is Loadable.Failed
            if (!known || alreadyValid) state.only() else state.selectAddress(action.addressId)
        }
        // Results carry the id they were computed for, so late answers for an old address are ignored.
        is AddressAction.Validated ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Ready(action.result))).only()
        is AddressAction.ValidationFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Failed(action.error))).only()
    }
}

internal class ShippingReducer @Inject constructor() : CheckoutSliceReducer<ShippingAction> {
    override fun reduce(state: CheckoutState, action: ShippingAction): CheckoutTransition = when (action) {
        is ShippingAction.Selected ->
            if (state.availableShippingOptions.none { it.id == action.optionId }) state.only()
            else state.copy(shipping = state.shipping.copy(selectedId = action.optionId)).only()
        ShippingAction.RetryClicked -> state.requestQuotes()
        is ShippingAction.QuotesLoaded ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Ready(action.options))).only()
                .then { it.reconcileShippingSelection() }
        is ShippingAction.QuotesFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Failed(action.error))).only()
    }
}

internal class PromoReducer @Inject constructor() : CheckoutSliceReducer<PromoAction> {
    override fun reduce(state: CheckoutState, action: PromoAction): CheckoutTransition {
        val promo = state.promo
        return when (action) {
            is PromoAction.InputChanged -> state.copy(
                promo = promo.copy(
                    input = action.text,
                    status = if (promo.status is PromoStatus.Rejected) PromoStatus.None else promo.status,
                )
            ).only()

            PromoAction.ApplyClicked -> {
                val code = promo.input.trim()
                val cart = state.contextOrNull?.cart
                val busy = promo.status is PromoStatus.Redeeming || promo.status is PromoStatus.Applied
                if (code.isEmpty() || cart == null || cart.isEmpty || busy) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Redeeming(code)))
                    .withEffects(CheckoutEffect.RedeemPromo(code, cart))
            }

            PromoAction.RemoveClicked -> state.copy(promo = PromoSlice()).only()

            // Only accept results while we are still waiting for one (the user may have removed it).
            is PromoAction.Redeemed ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Applied(action.promo))).only()

            is PromoAction.Rejected ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Rejected(action.error))).only()
        }
    }
}

internal class PaymentReducer @Inject constructor() : CheckoutSliceReducer<PaymentAction> {
    override fun reduce(state: CheckoutState, action: PaymentAction): CheckoutTransition = when (action) {
        is PaymentAction.Selected ->
            if (state.contextOrNull?.paymentMethods?.none { it.id == action.paymentMethodId } != false) state.only()
            else state.copy(selectedPaymentId = action.paymentMethodId).only()
    }
}

internal class OrderReducer @Inject constructor() : CheckoutSliceReducer<OrderAction> {
    override fun reduce(state: CheckoutState, action: OrderAction): CheckoutTransition = when (action) {
        OrderAction.PlaceClicked -> {
            val request = state.orderRequestOrNull()
            if (request == null) state.only()
            else state.copy(submission = Submission.Submitting).withEffects(CheckoutEffect.PlaceOrder(request))
        }
        is OrderAction.Placed ->
            state.copy(submission = Submission.Idle)
                .withEvent(CheckoutEvent.NavigateToConfirmation(action.order.id))
        is OrderAction.Failed -> {
            val failed = state.copy(submission = Submission.Failed(action.error)).only()
            // Prices moved under us: re-quote so the user sees (and accepts) the new total.
            if (action.error == DomainError.PriceChanged) failed.then { it.requestQuotes() } else failed
        }
        OrderAction.ErrorDismissed -> state.copy(submission = Submission.Idle).only()
    }
}
