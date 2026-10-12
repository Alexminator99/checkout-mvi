package com.example.feature.checkout.effect

import com.example.core.mvi.EffectHandler
import com.example.domain.error.DomainError
import com.example.domain.error.Outcome
import com.example.domain.error.fold
import com.example.domain.usecase.GetShippingQuotesUseCase
import com.example.domain.usecase.ObserveCheckoutContextUseCase
import com.example.domain.usecase.PlaceOrderUseCase
import com.example.domain.usecase.RedeemPromoUseCase
import com.example.domain.usecase.UpdateCartQuantityUseCase
import com.example.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.ContextAction
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.ShippingAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The bridge between the pure core and the outside world.
 *
 * Its only job: effect -> use case call -> action. No decisions live here
 * (those are in the reducer), and no I/O details either (those are behind the
 * domain ports). Depends on use cases only, never on repositories.
 */
internal class CheckoutEffectHandler @Inject constructor(
    private val observeContext: ObserveCheckoutContextUseCase,
    private val updateQuantity: UpdateCartQuantityUseCase,
    private val validateAddress: ValidateAddressUseCase,
    private val getShippingQuotes: GetShippingQuotesUseCase,
    private val redeemPromo: RedeemPromoUseCase,
    private val placeOrder: PlaceOrderUseCase,
) : EffectHandler<CheckoutEffect, CheckoutAction> {

    override fun handle(effect: CheckoutEffect): Flow<CheckoutAction> = when (effect) {

        CheckoutEffect.ObserveContext ->
            observeContext().map { outcome -> outcome.fold(ContextAction::Updated, ContextAction::Failed) }

        is CheckoutEffect.UpdateQuantity -> once(
            call = { updateQuantity(effect.sku, effect.quantity) },
            onSuccess = { null },                       // the cart stream reports the new state
            onFailure = CartAction::UpdateFailed,
        )

        is CheckoutEffect.ValidateAddress -> once(
            call = { validateAddress(effect.addressId) },
            onSuccess = { AddressAction.Validated(effect.addressId, it) },
            onFailure = { AddressAction.ValidationFailed(effect.addressId, it) },
        )

        is CheckoutEffect.LoadShippingQuotes -> once(
            call = { getShippingQuotes(effect.addressId, effect.cart) },
            onSuccess = { ShippingAction.QuotesLoaded(effect.addressId, it) },
            onFailure = { ShippingAction.QuotesFailed(effect.addressId, it) },
        )

        is CheckoutEffect.RedeemPromo -> once(
            call = { redeemPromo(effect.code, effect.cart) },
            onSuccess = PromoAction::Redeemed,
            onFailure = PromoAction::Rejected,
        )

        is CheckoutEffect.PlaceOrder -> once(
            call = { placeOrder(effect.request) },
            onSuccess = OrderAction::Placed,
            onFailure = OrderAction::Failed,
        )
    }

    /** One-shot call -> at most one action. */
    private fun <T> once(
        call: suspend () -> Outcome<T>,
        onSuccess: (T) -> CheckoutAction?,
        onFailure: (DomainError) -> CheckoutAction,
    ): Flow<CheckoutAction> = flow {
        call().fold(onSuccess, onFailure)?.let { emit(it) }
    }
}
