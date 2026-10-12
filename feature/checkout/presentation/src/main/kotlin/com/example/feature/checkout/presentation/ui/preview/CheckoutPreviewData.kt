package com.example.feature.checkout.presentation.ui.preview

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.ui.model.AddressSectionUi
import com.example.feature.checkout.presentation.ui.model.AddressStatusUi
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import com.example.feature.checkout.presentation.ui.model.SummaryUi

/** Sample UI state for previews only. Amounts arrive pre-formatted, as a projector delivers them. */
internal object CheckoutPreviewData {

    val cart = CartSectionUi(
        title = UiText.plural(R.plurals.checkout_item_count, 3, 3),
        lines = listOf(
            CartLineUi("mug", "Coffee mug", 2, "€24.00", canDecrement = true, decrementRemovesLine = false, canIncrement = true),
            CartLineUi("tee", "T-shirt", 1, "€20.00", canDecrement = true, decrementRemovesLine = true, canIncrement = true),
        ),
        isEmpty = false,
    )

    val address = AddressSectionUi(
        rows = listOf(
            SelectableRowUi("home", UiText.Raw("Home"), UiText.Raw("Calle Larios 1, 29005 Málaga"), trailing = null, selected = true),
            SelectableRowUi("office", UiText.Raw("Office"), UiText.Raw("Av. de Andalucía 10, 29007 Málaga"), trailing = null, selected = false),
        ),
        status = AddressStatusUi.Deliverable,
    )

    val shipping = ShippingSectionUi.Options(
        rows = listOf(
            SelectableRowUi(
                id = "standard",
                title = UiText.Raw("Standard"),
                subtitle = UiText.plural(R.plurals.checkout_delivery_days, 3, 3),
                trailing = UiText.res(R.string.checkout_free_for_members),
                selected = true,
            ),
            SelectableRowUi(
                id = "express",
                title = UiText.Raw("Express"),
                subtitle = UiText.plural(R.plurals.checkout_delivery_days, 1, 1),
                trailing = UiText.Raw("€12.99"),
                selected = false,
            ),
        ),
    )

    val promo = PromoSectionUi(
        input = "SAVE5",
        inputEnabled = true,
        applyEnabled = true,
        isRedeeming = false,
        applied = null,
        error = null,
    )

    val payment = PaymentSectionUi(
        rows = listOf(
            SelectableRowUi("visa", UiText.res(R.string.checkout_card_label, "Visa", "4242"), subtitle = null, trailing = null, selected = true),
            SelectableRowUi("mastercard", UiText.res(R.string.checkout_card_label, "Mastercard", "0002"), subtitle = null, trailing = null, selected = false),
        ),
    )

    val summary = SummaryUi(
        rows = listOf(
            SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw("€44.00")),
            SummaryRowUi(UiText.res(R.string.checkout_shipping), UiText.res(R.string.checkout_free)),
        ),
        total = "€44.00",
    )

    val placeOrder = PlaceOrderButtonUi(label = UiText.res(R.string.checkout_pay_amount, "€44.00"), enabled = true, loading = false)

    val content = CheckoutUiState.Content(
        isOffline = false,
        cart = cart,
        address = address,
        shipping = shipping,
        promo = promo,
        payment = payment,
        summary = summary,
        placeOrder = placeOrder,
        blockingError = null,
    )
}
