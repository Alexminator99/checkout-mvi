package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingReducerTest {

    private val reducer = ShippingReducer()
    private val quotes = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `picking an offered option selects it`() {
        assertEquals(EXPRESS.id, reducer.reduce(readyState(), ShippingAction.Selected(EXPRESS.id)).state.shipping.selectedId)
    }

    @Test
    fun `picking an option that is not offered is ignored`() {
        val expressOff = readyState(checkoutContext(isExpressShippingEnabled = false))

        assertEquals(expressOff, reducer.reduce(expressOff, ShippingAction.Selected(EXPRESS.id)).state)
    }

    @Test
    fun `loaded quotes keep the current pick while it is still offered`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading, selectedId = EXPRESS.id))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(EXPRESS.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `loaded quotes otherwise pick the cheapest option for the customer`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading, selectedId = "gone"))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(PICKUP.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `members get free standard shipping rather than the slower pickup`() {
        val loading = readyState(checkoutContext(customer = MEMBER)).copy(shipping = ShippingSlice(Loadable.Loading))

        val transition = reducer.reduce(loading, ShippingAction.QuotesLoaded(HOME.id, quotes))

        assertEquals(STANDARD.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `late quotes for a previous address are ignored`() {
        val start = readyState().copy(
            address = AddressSlice(OFFICE.id, Loadable.Loading),
            shipping = ShippingSlice(Loadable.Loading),
        )

        assertEquals(start, reducer.reduce(start, ShippingAction.QuotesLoaded(HOME.id, listOf(PICKUP))).state)
        assertEquals(start, reducer.reduce(start, ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network)).state)
    }

    @Test
    fun `quotes arriving when none were requested are ignored`() {
        val idle = readyState().copy(shipping = ShippingSlice())

        assertEquals(idle, reducer.reduce(idle, ShippingAction.QuotesLoaded(HOME.id, quotes)).state)
    }

    @Test
    fun `failed quotes arriving when none were requested are ignored`() {
        val idle = readyState().copy(shipping = ShippingSlice())

        assertEquals(idle, reducer.reduce(idle, ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network)).state)
    }

    @Test
    fun `failed quotes for the current address are shown`() {
        val loading = readyState().copy(shipping = ShippingSlice(Loadable.Loading))

        val transition = reducer.reduce(loading, ShippingAction.QuotesFailed(HOME.id, CheckoutError.Network))

        assertEquals(Loadable.Failed(CheckoutError.Network), transition.state.shipping.quotes)
    }

    @Test
    fun `retrying asks for quotes again`() {
        val transition = reducer.reduce(readyState(), ShippingAction.RetryClicked)

        assertEquals(Loadable.Loading, transition.state.shipping.quotes)
        assertEquals(listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, cartOf(MUG))), transition.effects)
    }
}
