package com.example.feature.checkout.domain.fixture

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import java.util.Currency

val EUR: Currency = Currency.getInstance("EUR")

fun eur(amount: String): Money = Money.of(amount, "EUR")

val HOME = Address("home", "Home", "Alex Doe", "Calle Larios 1", "Málaga", "29005", isDefault = true)
val OFFICE = Address("office", "Office", "Alex Doe", "Av. de Andalucía 10", "Málaga", "29007", isDefault = false)

val VISA = PaymentMethod("visa", "Visa", "4242", isDefault = true)
val MASTERCARD = PaymentMethod("mastercard", "Mastercard", "0002", isDefault = false)

val STANDARD = ShippingOption("standard", "Standard", eur("4.99"), estimatedDays = 3, isExpress = false)
val PICKUP = ShippingOption("pickup", "Pickup point", eur("2.99"), estimatedDays = 4, isExpress = false)
val EXPRESS = ShippingOption("express", "Express", eur("12.99"), estimatedDays = 1, isExpress = true)

val SAVE5 = Promo("SAVE5", eur("5.00"))

val MUG = CartLine("mug", "Mug", eur("12.00"), quantity = 2)
val TEE = CartLine("tee", "T-shirt", eur("20.00"), quantity = 1)

val CUSTOMER = Customer("customer-1", "Alex", isMember = false)
val MEMBER = Customer("customer-1", "Alex", isMember = true)

fun cartOf(vararg lines: CartLine): Cart = Cart(lines.toList(), EUR)

fun checkoutContext(
    cart: Cart = cartOf(MUG),
    addresses: List<Address> = listOf(HOME, OFFICE),
    paymentMethods: List<PaymentMethod> = listOf(VISA, MASTERCARD),
    customer: Customer = CUSTOMER,
    isOnline: Boolean = true,
    isExpressShippingEnabled: Boolean = true,
): CheckoutContext = CheckoutContext(cart, addresses, paymentMethods, customer, isOnline, isExpressShippingEnabled)
