package com.example.feature.checkout.presentation.effect

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.CheckoutTestEnvironment
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutEffectHandlerTest {

    private val environment = CheckoutTestEnvironment()
    private val handler = environment.effectHandler()
    private val request = OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null)

    private suspend fun results(effect: CheckoutEffect): List<CheckoutAction> = handler.handle(effect).toList()

    @Test
    fun `observing the context reports the combined context`() = runTest {
        assertEquals(ContextAction.Updated(checkoutContext()), handler.handle(CheckoutEffect.ObserveContext).first())
    }

    @Test
    fun `a failed context load is reported`() = runTest {
        environment.addresses.addresses = Outcome.Failure(CheckoutError.Network)

        assertEquals(ContextAction.Failed(CheckoutError.Network), handler.handle(CheckoutEffect.ObserveContext).first())
    }

    @Test
    fun `a quantity write reports nothing because the cart stream does`() = runTest {
        assertEquals(emptyList<CheckoutAction>(), results(CheckoutEffect.UpdateQuantity(MUG.sku, 3)))
        assertEquals(3, environment.cart.cart.value.lines.single().quantity)
    }

    @Test
    fun `a failed quantity write is reported`() = runTest {
        environment.cart.failure = CheckoutError.Network

        assertEquals(listOf(CartAction.UpdateFailed(CheckoutError.Network)), results(CheckoutEffect.UpdateQuantity(MUG.sku, 3)))
    }

    @Test
    fun `an address check reports its result for that address`() = runTest {
        assertEquals(
            listOf(AddressAction.Validated(HOME.id, AddressValidation.Deliverable)),
            results(CheckoutEffect.ValidateAddress(HOME.id)),
        )
    }

    @Test
    fun `a failed address check is reported for that address`() = runTest {
        environment.addresses.validation = Outcome.Failure(CheckoutError.Network)

        assertEquals(
            listOf(AddressAction.ValidationFailed(HOME.id, CheckoutError.Network)),
            results(CheckoutEffect.ValidateAddress(HOME.id)),
        )
    }

    @Test
    fun `quotes are reported for their address`() = runTest {
        assertEquals(
            listOf(ShippingAction.QuotesLoaded(HOME.id, listOf(STANDARD, PICKUP, EXPRESS))),
            results(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))),
        )
    }

    @Test
    fun `failed quotes are reported for their address`() = runTest {
        environment.shipping.quotes = Outcome.Failure(CheckoutError.Network)

        assertEquals(
            listOf(ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network)),
            results(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))),
        )
    }

    @Test
    fun `a redeemed promo is reported`() = runTest {
        assertEquals(listOf(PromoAction.Redeemed(SAVE5)), results(CheckoutEffect.RedeemPromo("SAVE5", cartOf(MUG))))
    }

    @Test
    fun `a rejected promo is reported`() = runTest {
        environment.promos.result = Outcome.Failure(CheckoutError.Promo.Expired)

        assertEquals(
            listOf(PromoAction.Rejected(CheckoutError.Promo.Expired)),
            results(CheckoutEffect.RedeemPromo("OLD", cartOf(MUG))),
        )
    }

    @Test
    fun `a placed order is reported`() = runTest {
        assertEquals(listOf(OrderAction.Placed(Order("order-1"))), results(CheckoutEffect.PlaceOrder(request)))
    }

    @Test
    fun `a failed order is reported`() = runTest {
        environment.orders.result = Outcome.Failure(CheckoutError.Payment.Declined)

        assertEquals(listOf(OrderAction.Failed(CheckoutError.Payment.Declined)), results(CheckoutEffect.PlaceOrder(request)))
    }
}
