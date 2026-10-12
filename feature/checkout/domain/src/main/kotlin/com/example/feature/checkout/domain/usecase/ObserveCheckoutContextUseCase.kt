package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.AddressRepository
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.repository.CustomerRepository
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import com.example.feature.checkout.domain.repository.NetworkMonitor
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Everything the checkout depends on, combined into one read model that updates whenever a source does. */
class ObserveCheckoutContextUseCase @Inject constructor(
    private val cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    operator fun invoke(): Flow<CheckoutOutcome<CheckoutContext>> {
        val environment = combine(
            customerRepository.observeCustomer(),
            networkMonitor.isOnline,
            featureFlagRepository.observe(FeatureFlag.EXPRESS_SHIPPING),
            ::Environment,
        )
        return combine(
            cartRepository.observeCart(),
            addressRepository.observeAddresses(),
            paymentMethodRepository.observePaymentMethods(),
            environment,
            ::checkoutContext,
        ).distinctUntilChanged()
    }

    private fun checkoutContext(
        cart: Cart,
        addresses: CheckoutOutcome<List<Address>>,
        paymentMethods: CheckoutOutcome<List<PaymentMethod>>,
        environment: Environment,
    ): CheckoutOutcome<CheckoutContext> = when (addresses) {
        is Outcome.Failure -> addresses
        is Outcome.Success -> when (paymentMethods) {
            is Outcome.Failure -> paymentMethods
            is Outcome.Success -> Outcome.Success(
                CheckoutContext(
                    cart = cart,
                    addresses = addresses.value,
                    paymentMethods = paymentMethods.value,
                    customer = environment.customer,
                    isOnline = environment.isOnline,
                    isExpressShippingEnabled = environment.isExpressShippingEnabled,
                )
            )
        }
    }

    private data class Environment(
        val customer: Customer,
        val isOnline: Boolean,
        val isExpressShippingEnabled: Boolean,
    )
}
