package com.example.feature.checkout.presentation

import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fake.FakeCustomerRepository
import com.example.feature.checkout.domain.fake.FakeFeatureFlagRepository
import com.example.feature.checkout.domain.fake.FakeNetworkMonitor
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fake.FakePaymentMethodRepository
import com.example.feature.checkout.domain.fake.FakePromoRepository
import com.example.feature.checkout.domain.fake.FakeShippingRepository
import com.example.feature.checkout.domain.usecase.GetShippingQuotesUseCase
import com.example.feature.checkout.domain.usecase.ObserveCheckoutContextUseCase
import com.example.feature.checkout.domain.usecase.PlaceOrderUseCase
import com.example.feature.checkout.domain.usecase.RedeemPromoUseCase
import com.example.feature.checkout.domain.usecase.UpdateCartQuantityUseCase
import com.example.feature.checkout.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.presentation.effect.CheckoutEffectHandler

/** Every repository the checkout uses, as fakes, and the effect handler wired to real use cases over them. */
internal class CheckoutTestEnvironment(
    val cart: FakeCartRepository = FakeCartRepository(),
    val addresses: FakeAddressRepository = FakeAddressRepository(),
    val shipping: FakeShippingRepository = FakeShippingRepository(),
    val promos: FakePromoRepository = FakePromoRepository(),
    val paymentMethods: FakePaymentMethodRepository = FakePaymentMethodRepository(),
    val orders: FakeOrderRepository = FakeOrderRepository(),
    val customer: FakeCustomerRepository = FakeCustomerRepository(),
    val network: FakeNetworkMonitor = FakeNetworkMonitor(),
    val flags: FakeFeatureFlagRepository = FakeFeatureFlagRepository(),
) {
    fun effectHandler(): CheckoutEffectHandler = CheckoutEffectHandler(
        observeCheckoutContext = ObserveCheckoutContextUseCase(cart, addresses, paymentMethods, customer, network, flags),
        updateCartQuantity = UpdateCartQuantityUseCase(cart),
        validateAddress = ValidateAddressUseCase(addresses),
        getShippingQuotes = GetShippingQuotesUseCase(shipping),
        redeemPromo = RedeemPromoUseCase(promos),
        placeOrder = PlaceOrderUseCase(orders),
    )
}
