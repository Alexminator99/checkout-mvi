package com.example.domain.model

data class CartLine(
    val sku: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val lineTotal: Money get() = unitPrice * quantity
}

data class Cart(val lines: List<CartLine>, val currency: java.util.Currency) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Money get() = lines.fold(Money.zero(currency)) { acc, line -> acc + line.lineTotal }
}

data class Address(
    val id: String,
    val recipient: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean,
)

sealed interface AddressValidation {
    data object Deliverable : AddressValidation
    data class Undeliverable(val reason: Reason) : AddressValidation
    enum class Reason { OUTSIDE_SERVICE_AREA, INCOMPLETE, PO_BOX_NOT_ALLOWED }
}

data class ShippingOption(
    val id: String,
    val label: String,
    val price: Money,
    val estimatedDays: Int,
    val isExpress: Boolean,
)

data class Promo(val code: String, val discount: Money)

data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)

data class Customer(val id: String, val displayName: String, val isPremium: Boolean)

data class Order(val id: String)

/** Everything the checkout screen observes from the outside world, as one read model. */
data class CheckoutContext(
    val cart: Cart,
    val addresses: List<Address>,
    val paymentMethods: List<PaymentMethod>,
    val customer: Customer,
    val isOnline: Boolean,
    val isExpressShippingEnabled: Boolean,
)

data class OrderRequest(
    val addressId: String,
    val shippingOptionId: String,
    val paymentMethodId: String,
    val promoCode: String?,
)

data class OrderTotals(
    val subtotal: Money,
    val shipping: Money,
    val discount: Money,
    val total: Money,
)

enum class FeatureFlag { EXPRESS_SHIPPING }
