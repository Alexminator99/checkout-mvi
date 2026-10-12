package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.AddressValidation.Reason
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressValidationMapperTest {

    @Test
    fun `a deliverable answer maps to Deliverable`() {
        assertEquals(AddressValidation.Deliverable, AddressValidationDto(deliverable = true, reasonCode = null).toDomain())
    }

    @Test
    fun `reason codes map to reasons`() {
        assertEquals(undeliverable(Reason.PO_BOX_NOT_ALLOWED), refused("PO_BOX"))
        assertEquals(undeliverable(Reason.INCOMPLETE), refused("INCOMPLETE"))
        assertEquals(undeliverable(Reason.OUTSIDE_SERVICE_AREA), refused("OUTSIDE_AREA"))
        assertEquals(undeliverable(Reason.OUTSIDE_SERVICE_AREA), refused(null))
    }

    private fun refused(reasonCode: String?) = AddressValidationDto(deliverable = false, reasonCode = reasonCode).toDomain()

    private fun undeliverable(reason: Reason) = AddressValidation.Undeliverable(reason)
}
