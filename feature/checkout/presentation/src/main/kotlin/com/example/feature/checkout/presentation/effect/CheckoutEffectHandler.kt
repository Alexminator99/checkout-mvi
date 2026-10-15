package com.example.feature.checkout.presentation.effect

import com.example.core.domain.fold
import com.example.core.mvi.EffectHandler
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.usecase.GetShippingQuotesUseCase
import com.example.feature.checkout.domain.usecase.ObserveCheckoutContextUseCase
import com.example.feature.checkout.domain.usecase.PlaceOrderUseCase
import com.example.feature.checkout.domain.usecase.RedeemPromoUseCase
import com.example.feature.checkout.domain.usecase.RefreshCartUseCase
import com.example.feature.checkout.domain.usecase.ResetDemoUseCase
import com.example.feature.checkout.domain.usecase.UpdateCartQuantityUseCase
import com.example.feature.checkout.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * The bridge between the pure reducers and the outside world: effect, then one use case call, then
 * the action that reports the result. Decisions live in the reducers; I/O lives behind the use cases.
 */
internal class CheckoutEffectHandler @Inject constructor(
    private val observeCheckoutContext: ObserveCheckoutContextUseCase,
    private val updateCartQuantity: UpdateCartQuantityUseCase,
    private val validateAddress: ValidateAddressUseCase,
    private val getShippingQuotes: GetShippingQuotesUseCase,
    private val redeemPromo: RedeemPromoUseCase,
    private val placeOrder: PlaceOrderUseCase,
    private val refreshCart: RefreshCartUseCase,
    private val resetDemo: ResetDemoUseCase,
) : EffectHandler<CheckoutEffect, CheckoutAction> {

    override fun handle(effect: CheckoutEffect): Flow<CheckoutAction> = when (effect) {
        CheckoutEffect.ObserveContext ->
            observeCheckoutContext().map { outcome -> outcome.fold(ContextAction::Updated, ContextAction::Failed) }

        CheckoutEffect.RefreshCart -> once(
            call = { refreshCart() },
            onSuccess = OrderAction::PricesRefreshed,
            onFailure = OrderAction::PriceRefreshFailed,
        )
        CheckoutEffect.ResetDemo -> once(
            call = { resetDemo() },
            onSuccess = { LifecycleAction.DemoReset },
            onFailure = LifecycleAction::DemoResetFailed,
        )

        is CheckoutEffect.UpdateQuantity -> once(
            call = { updateCartQuantity(effect.sku, effect.quantity) },
            onSuccess = CartAction::Updated,
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

    /** A one-shot call that reports at most one action. */
    private fun <T> once(
        call: suspend () -> CheckoutOutcome<T>,
        onSuccess: (T) -> CheckoutAction?,
        onFailure: (CheckoutError) -> CheckoutAction,
    ): Flow<CheckoutAction> = flow {
        call().fold(onSuccess, onFailure)?.let { emit(it) }
    }
}
