package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import javax.inject.Inject

internal class AddressSectionProjector @Inject constructor() {

    fun project(ready: ReadyCheckout): AddressSectionUi {
        val slice = ready.state.address
        return AddressSectionUi(
            rows = ready.context.addresses.map { address ->
                SelectableRowUi(
                    id = address.id,
                    title = UiText.Raw(address.label),
                    subtitle = UiText.res(R.string.checkout_address_line, address.line1, address.postalCode, address.city),
                    trailing = null,
                    selected = address.id == slice.selectedId,
                )
            },
            status = when (val validation = slice.validation) {
                Loadable.Idle -> AddressStatusUi.None
                Loadable.Loading -> AddressStatusUi.Checking
                is Loadable.Failed -> AddressStatusUi.Warning(validation.error.toUiText())
                is Loadable.Ready -> when (val result = validation.value) {
                    AddressValidation.Deliverable -> AddressStatusUi.Deliverable
                    is AddressValidation.Undeliverable -> AddressStatusUi.Warning(result.reason.toUiText())
                }
            },
        )
    }
}
