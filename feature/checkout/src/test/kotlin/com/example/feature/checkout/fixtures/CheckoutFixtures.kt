package com.example.feature.checkout.fixtures

import com.example.core.ui.MoneyFormatter
import com.example.domain.error.Outcome
import com.example.domain.model.Address
import com.example.domain.model.AddressValidation
import com.example.domain.model.Cart
import com.example.domain.model.CartLine
import com.example.domain.model.CheckoutContext
import com.example.domain.model.Customer
import com.example.domain.model.FeatureFlag
import com.example.domain.model.Money
import com.example.domain.model.Order
import com.example.domain.model.OrderRequest
import com.example.domain.model.PaymentMethod
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
import com.example.feature.checkout.contract.AddressSlice
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.ShippingSlice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun eur(amount: String) = Money.of(amount, "EUR")
val EUR: java.util.Currency = java.util.Currency.getInstance("EUR")

val HOME = Address("home", "Alex", "Calle Larios 1", "Málaga", "29005", isDefault = true)
val OFFICE = Address("office", "Alex", "Av. Andalucía 10", "Málaga", "29007", isDefault = false)
val VISA = PaymentMethod("visa", "Visa", "4242", isDefault = true)
val STANDARD = ShippingOption("std", "Standard", eur("4.99"), estimatedDays = 3, isExpress = false)
val EXPRESS = ShippingOption("exp", "Express", eur("12.99"), estimatedDays = 1, isExpress = true)
val PICKUP = ShippingOption("pickup", "Pickup point", eur("2.99"), estimatedDays = 4, isExpress = false)
val SAVE5 = Promo("SAVE5", eur("5.00"))

fun cartOf(vararg lines: CartLine) = Cart(lines.toList(), EUR)
val MUG = CartLine("mug", "Mug", eur("12.00"), quantity = 2)
val TEE = CartLine("tee", "T-shirt", eur("20.00"), quantity = 1)

fun context(
    cart: Cart = cartOf(MUG),
    addresses: List<Address> = listOf(HOME, OFFICE),
    payments: List<PaymentMethod> = listOf(VISA),
    premium: Boolean = false,
    online: Boolean = true,
    express: Boolean = true,
) = CheckoutContext(cart, addresses, payments, Customer("c1", "Alex", premium), online, express)

/** A state that satisfies every precondition for placing an order. */
fun readyState(ctx: CheckoutContext = context()) = CheckoutState(
    context = Loadable.Ready(ctx),
    address = AddressSlice(selectedId = HOME.id, validation = Loadable.Ready(AddressValidation.Deliverable)),
    shipping = ShippingSlice(quotes = Loadable.Ready(listOf(STANDARD, EXPRESS)), selectedId = STANDARD.id),
    selectedPaymentId = VISA.id,
)

/** Deterministic formatter: tests should not depend on the JVM's default locale. */
val TestMoney = MoneyFormatter { "€" + it.amount.toPlainString() }

/**
 * One in-memory implementation of every port. Configure outcomes per test;
 * calls are recorded so tests can assert on what reached the "backend".
 */
class FakePorts(
    initialCart: Cart = cartOf(MUG),
    var addresses: Outcome<List<Address>> = Outcome.Success(listOf(HOME, OFFICE)),
    var payments: Outcome<List<PaymentMethod>> = Outcome.Success(listOf(VISA)),
    var validation: Outcome<AddressValidation> = Outcome.Success(AddressValidation.Deliverable),
    var quotes: Outcome<List<ShippingOption>> = Outcome.Success(listOf(STANDARD, EXPRESS)),
    var promoResult: Outcome<Promo> = Outcome.Success(SAVE5),
    var orderResult: Outcome<Order> = Outcome.Success(Order("order-1")),
) : CartReader, CartWriter, AddressRepository, ShippingRepository, PromoRepository,
    PaymentMethodRepository, OrderRepository, CustomerRepository, NetworkMonitor, FeatureFlagRepository {

    val cart = MutableStateFlow(initialCart)
    val online = MutableStateFlow(true)
    val expressEnabled = MutableStateFlow(true)
    val placedOrders = mutableListOf<OrderRequest>()
    val quoteRequests = mutableListOf<String>()

    override fun observeCart(): Flow<Cart> = cart
    override suspend fun setQuantity(sku: String, quantity: Int): Outcome<Unit> {
        cart.value = cart.value.copy(lines = cart.value.lines.map { if (it.sku == sku) it.copy(quantity = quantity) else it })
        return Outcome.Success(Unit)
    }
    override suspend fun removeLine(sku: String): Outcome<Unit> {
        cart.value = cart.value.copy(lines = cart.value.lines.filterNot { it.sku == sku })
        return Outcome.Success(Unit)
    }
    override fun observeAddresses(): Flow<Outcome<List<Address>>> = MutableStateFlow(addresses)
    override suspend fun validate(addressId: String) = validation
    override suspend fun quote(addressId: String, cart: Cart): Outcome<List<ShippingOption>> {
        quoteRequests += addressId
        return quotes
    }
    override suspend fun redeem(code: String, cart: Cart) = promoResult
    override fun observePaymentMethods(): Flow<Outcome<List<PaymentMethod>>> = MutableStateFlow(payments)
    override suspend fun place(request: OrderRequest): Outcome<Order> {
        placedOrders += request
        return orderResult
    }
    override fun observeCustomer(): Flow<Customer> = MutableStateFlow(Customer("c1", "Alex", isPremium = false))
    override val isOnline: Flow<Boolean> get() = online
    override fun observe(flag: FeatureFlag): Flow<Boolean> = expressEnabled.map { it }
}
