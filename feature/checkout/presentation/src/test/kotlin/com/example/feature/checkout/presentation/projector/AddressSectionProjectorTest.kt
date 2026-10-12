package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressSectionProjectorTest {

    private val projector = AddressSectionProjector()

    @Test
    fun `each address shows its label and address line, with the selection marked`() {
        val home = projector.project(readyCheckout()).rows.first { it.id == HOME.id }

        assertEquals(
            SelectableRowUi(
                id = HOME.id,
                title = UiText.Raw(HOME.label),
                subtitle = UiText.res(R.string.checkout_address_line, HOME.line1, HOME.postalCode, HOME.city),
                trailing = null,
                selected = true,
            ),
            home,
        )
    }

    @Test
    fun `the check status is described in words`() {
        val poBox = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)

        assertEquals(AddressStatusUi.None, statusFor(Loadable.Idle))
        assertEquals(AddressStatusUi.Checking, statusFor(Loadable.Loading))
        assertEquals(AddressStatusUi.Deliverable, statusFor(Loadable.Ready(AddressValidation.Deliverable)))
        assertEquals(AddressStatusUi.Warning(UiText.res(R.string.address_po_box)), statusFor(Loadable.Ready(poBox)))
        assertEquals(AddressStatusUi.Warning(UiText.res(R.string.error_network)), statusFor(Loadable.Failed(CheckoutError.Network)))
    }

    private fun statusFor(validation: Loadable<AddressValidation>): AddressStatusUi =
        projector.project(readyCheckout(readyState().copy(address = AddressSlice(HOME.id, validation)))).status
}
