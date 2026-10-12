package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressReducerTest {

    private val reducer = AddressReducer()

    @Test
    fun `picking another address restarts its check and the quotes`() {
        val transition = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id))

        assertEquals(AddressSlice(OFFICE.id, Loadable.Loading), transition.state.address)
        assertNull(transition.state.shipping.selectedId)
        assertEquals(
            listOf(CheckoutEffect.ValidateAddress(OFFICE.id), CheckoutEffect.LoadShippingQuotes(OFFICE.id, cartOf(MUG))),
            transition.effects,
        )
    }

    @Test
    fun `picking the current, valid address changes nothing`() {
        assertEquals(readyState(), reducer.reduce(readyState(), AddressAction.Selected(HOME.id)).state)
    }

    @Test
    fun `picking the address again after a failed check retries it`() {
        val failed = readyState().copy(address = AddressSlice(HOME.id, Loadable.Failed(CheckoutError.Network)))

        val transition = reducer.reduce(failed, AddressAction.Selected(HOME.id))

        assertTrue(CheckoutEffect.ValidateAddress(HOME.id) in transition.effects)
    }

    @Test
    fun `an unknown address is ignored`() {
        assertTrue(reducer.reduce(readyState(), AddressAction.Selected("nowhere")).effects.isEmpty())
    }

    @Test
    fun `the check result for the current address is stored`() {
        val checking = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state
        val undeliverable = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)

        val transition = reducer.reduce(checking, AddressAction.Validated(OFFICE.id, undeliverable))

        assertEquals(Loadable.Ready(undeliverable), transition.state.address.validation)
    }

    @Test
    fun `a late result for a previous address is ignored`() {
        val checking = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state

        assertEquals(checking, reducer.reduce(checking, AddressAction.Validated(HOME.id, AddressValidation.Deliverable)).state)
        assertEquals(checking, reducer.reduce(checking, AddressAction.ValidationFailed(HOME.id, CheckoutError.Network)).state)
    }
}
