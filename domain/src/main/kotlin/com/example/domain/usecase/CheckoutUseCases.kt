package com.example.domain.usecase

import com.example.domain.error.Outcome
import com.example.domain.model.AddressValidation
import com.example.domain.model.Cart
import com.example.domain.model.CheckoutContext
import com.example.domain.model.Customer
import com.example.domain.model.FeatureFlag
import com.example.domain.model.Money
import com.example.domain.model.Order
import com.example.domain.model.OrderRequest
import com.example.domain.model.OrderTotals
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption
import com.example.domain.repository.AddressRepository
import com.example.domain.repository.CartReader
import com.example.domain.repository.CartWriter
import com.example.domain.repository.CustomerRepository
import com.example.domain.repository.FeatureFlagRepository
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.OrderRepository
import com.example.domain.repository.PaymentMethodRepository
import com.example.domain.repository.PromoRepository
import com.example.domain.repository.ShippingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

// Use cases: one responsibility each, no mutable state, depend only on ports.
// They are concrete classes on purpose: presentation depends *inward* on the
// domain, which is what the Dependency Rule asks for. Tests swap the ports
// (fake repositories), not the use cases. javax.inject is a JSR-330 annotation
// jar, not Hilt or Android, so the domain module stays pure Kotlin.

/** Aggregates every stream the checkout screen depends on into one read model. */
class ObserveCheckoutContextUseCase @Inject constructor(
    private val cart: CartReader,
    private val addresses: AddressRepository,
    private val payments: PaymentMethodRepository,
    private val customer: CustomerRepository,
    private val network: NetworkMonitor,
    private val flags: FeatureFlagRepository,
) {
    operator fun invoke(): Flow<Outcome<CheckoutContext>> {
        val environment = combine(
            customer.observeCustomer(),
            network.isOnline,
            flags.observe(FeatureFlag.EXPRESS_SHIPPING),
        ) { customer, online, express -> Environment(customer, online, express) }

        return combine(
            cart.observeCart(),
            addresses.observeAddresses(),
            payments.observePaymentMethods(),
            environment,
        ) { cart, addressesOutcome, paymentsOutcome, env ->
            when {
                addressesOutcome is Outcome.Failure -> addressesOutcome
                paymentsOutcome is Outcome.Failure -> paymentsOutcome
                else -> Outcome.Success(
                    CheckoutContext(
                        cart = cart,
                        addresses = (addressesOutcome as Outcome.Success).value,
                        paymentMethods = (paymentsOutcome as Outcome.Success).value,
                        customer = env.customer,
                        isOnline = env.isOnline,
                        isExpressShippingEnabled = env.expressEnabled,
                    )
                )
            }
        }.distinctUntilChanged()
    }

    private data class Environment(val customer: Customer, val isOnline: Boolean, val expressEnabled: Boolean)
}

class UpdateCartQuantityUseCase @Inject constructor(private val cart: CartWriter) {
    suspend operator fun invoke(sku: String, quantity: Int): Outcome<Unit> = when {
        quantity <= 0 -> cart.removeLine(sku)
        else -> cart.setQuantity(sku, quantity.coerceAtMost(MAX_QUANTITY_PER_LINE))
    }

    companion object { const val MAX_QUANTITY_PER_LINE = 10 }
}

class ValidateAddressUseCase @Inject constructor(private val addresses: AddressRepository) {
    suspend operator fun invoke(addressId: String): Outcome<AddressValidation> = addresses.validate(addressId)
}

class GetShippingQuotesUseCase @Inject constructor(private val shipping: ShippingRepository) {
    suspend operator fun invoke(addressId: String, cart: Cart): Outcome<List<ShippingOption>> =
        shipping.quote(addressId, cart)
}

class RedeemPromoUseCase @Inject constructor(private val promos: PromoRepository) {
    suspend operator fun invoke(rawCode: String, cart: Cart): Outcome<Promo> =
        promos.redeem(normalize(rawCode), cart)

    companion object {
        /** Business rule for what a promo code *is*; reused anywhere a code is entered. */
        fun normalize(raw: String): String = raw.trim().uppercase()
    }
}

class PlaceOrderUseCase @Inject constructor(private val orders: OrderRepository) {
    suspend operator fun invoke(request: OrderRequest): Outcome<Order> = orders.place(request)
}

/**
 * Pure pricing rules, shared by every screen that shows a total.
 * Deciding what the total *is* belongs here; formatting it belongs to a projector.
 */
class CalculateOrderTotalsUseCase @Inject constructor() {
    operator fun invoke(cart: Cart, shipping: ShippingOption?, promo: Promo?, customer: Customer): OrderTotals {
        val zero = Money.zero(cart.currency)
        val subtotal = cart.subtotal
        val shippingCost = when {
            shipping == null -> zero
            customer.isPremium && !shipping.isExpress -> zero    // member perk
            else -> shipping.price
        }
        val discount = promo?.discount?.coerceAtMost(subtotal) ?: zero
        return OrderTotals(subtotal, shippingCost, discount, subtotal + shippingCost - discount)
    }
}
