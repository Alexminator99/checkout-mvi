package com.example.feature.checkout.projector

import com.example.core.mvi.Projector
import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.domain.model.AddressValidation
import com.example.domain.model.CheckoutContext
import com.example.domain.usecase.CalculateOrderTotalsUseCase
import com.example.domain.usecase.UpdateCartQuantityUseCase
import com.example.feature.checkout.R
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.PromoStatus
import com.example.feature.checkout.contract.Submission
import com.example.feature.checkout.contract.appliedPromo
import com.example.feature.checkout.contract.availableShippingOptions
import com.example.feature.checkout.contract.selectedShippingOption
import com.example.feature.checkout.ui.model.AddressSectionUi
import com.example.feature.checkout.ui.model.AddressStatusUi
import com.example.feature.checkout.ui.model.CartLineUi
import com.example.feature.checkout.ui.model.CartSectionUi
import com.example.feature.checkout.ui.model.PaymentSectionUi
import com.example.feature.checkout.ui.model.PromoSectionUi
import com.example.feature.checkout.ui.model.SelectableRowUi
import com.example.feature.checkout.ui.model.ShippingSectionUi
import com.example.feature.checkout.ui.model.SummaryRowUi
import com.example.feature.checkout.ui.model.SummaryUi
import javax.inject.Inject

/** Input for section projectors: only built once the context has loaded, so no null checks below. */
internal data class ReadyCheckout(val state: CheckoutState, val context: CheckoutContext)

internal typealias SectionProjector<U> = Projector<ReadyCheckout, U>

// One projector per section (Single Responsibility). Each is pure: same input, same output.

internal class CartSectionProjector @Inject constructor(
    private val money: MoneyFormatter,
) : SectionProjector<CartSectionUi> {
    override fun project(state: ReadyCheckout): CartSectionUi {
        val cart = state.context.cart
        val editable = state.state.submission != Submission.Submitting
        return CartSectionUi(
            title = UiText.plural(R.plurals.checkout_item_count, cart.itemCount, cart.itemCount),
            lines = cart.lines.map { line ->
                CartLineUi(
                    sku = line.sku,
                    name = line.name,
                    quantity = line.quantity,
                    lineTotal = money.format(line.lineTotal),
                    canDecrement = editable && line.quantity > 1,
                    canIncrement = editable && line.quantity < UpdateCartQuantityUseCase.MAX_QUANTITY_PER_LINE,
                )
            },
            isEmpty = cart.isEmpty,
        )
    }
}

internal class AddressSectionProjector @Inject constructor() : SectionProjector<AddressSectionUi> {
    override fun project(state: ReadyCheckout): AddressSectionUi {
        val slice = state.state.address
        return AddressSectionUi(
            rows = state.context.addresses.map { address ->
                SelectableRowUi(
                    id = address.id,
                    title = UiText.Raw(address.recipient),
                    subtitle = UiText.Raw("${address.line1}, ${address.postalCode} ${address.city}"),
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

internal class ShippingSectionProjector @Inject constructor(
    private val money: MoneyFormatter,
) : SectionProjector<ShippingSectionUi> {
    override fun project(state: ReadyCheckout): ShippingSectionUi = when (val quotes = state.state.shipping.quotes) {
        Loadable.Idle -> ShippingSectionUi.Hidden
        Loadable.Loading -> ShippingSectionUi.Loading
        is Loadable.Failed -> ShippingSectionUi.Error(quotes.error.toUiText())
        is Loadable.Ready -> ShippingSectionUi.Options(
            state.state.availableShippingOptions.map { option ->
                val freeForMember = state.context.customer.isPremium && !option.isExpress
                SelectableRowUi(
                    id = option.id,
                    title = UiText.Raw(option.label),
                    subtitle = UiText.plural(R.plurals.checkout_delivery_days, option.estimatedDays, option.estimatedDays),
                    trailing = if (freeForMember) UiText.res(R.string.checkout_free_for_members)
                    else UiText.Raw(money.format(option.price)),
                    selected = option.id == state.state.shipping.selectedId,
                )
            }
        )
    }
}

internal class PromoSectionProjector @Inject constructor(
    private val money: MoneyFormatter,
) : SectionProjector<PromoSectionUi> {
    override fun project(state: ReadyCheckout): PromoSectionUi {
        val promo = state.state.promo
        val idle = promo.status is PromoStatus.None || promo.status is PromoStatus.Rejected
        return PromoSectionUi(
            input = promo.input,
            inputEnabled = idle,
            applyEnabled = idle && promo.input.isNotBlank() && !state.context.cart.isEmpty,
            isRedeeming = promo.status is PromoStatus.Redeeming,
            applied = state.state.appliedPromo?.let {
                UiText.res(R.string.checkout_promo_applied, it.code, money.format(it.discount))
            },
            error = (promo.status as? PromoStatus.Rejected)?.error?.toUiText(),
        )
    }
}

internal class PaymentSectionProjector @Inject constructor() : SectionProjector<PaymentSectionUi> {
    override fun project(state: ReadyCheckout): PaymentSectionUi = PaymentSectionUi(
        rows = state.context.paymentMethods.map { method ->
            SelectableRowUi(
                id = method.id,
                title = UiText.Raw("${method.brand} •••• ${method.last4}"),
                subtitle = null,
                trailing = null,
                selected = method.id == state.state.selectedPaymentId,
            )
        }
    )
}

/**
 * Formats the totals. Computing them is a domain rule (shared with other screens),
 * so it is delegated to [CalculateOrderTotalsUseCase], a pure function with no I/O.
 */
internal class SummaryProjector @Inject constructor(
    private val money: MoneyFormatter,
    private val calculateTotals: CalculateOrderTotalsUseCase,
) : SectionProjector<SummaryUi> {
    override fun project(state: ReadyCheckout): SummaryUi {
        val shippingOption = state.state.selectedShippingOption
        val totals = calculateTotals(state.context.cart, shippingOption, state.state.appliedPromo, state.context.customer)
        return SummaryUi(
            rows = buildList {
                add(SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw(money.format(totals.subtotal))))
                add(
                    SummaryRowUi(
                        label = UiText.res(R.string.checkout_shipping),
                        value = when {
                            shippingOption == null -> UiText.res(R.string.checkout_shipping_pending)
                            totals.shipping.isZero -> UiText.res(R.string.checkout_free)
                            else -> UiText.Raw(money.format(totals.shipping))
                        },
                    )
                )
                if (!totals.discount.isZero) {
                    add(SummaryRowUi(UiText.res(R.string.checkout_discount), UiText.Raw("−" + money.format(totals.discount))))
                }
            },
            total = money.format(totals.total),
        )
    }
}
