package com.example.data.repository

import com.example.data.di.IoDispatcher
import com.example.data.error.safeCall
import com.example.data.local.CartDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toQuoteLines
import com.example.data.remote.CheckoutApi
import com.example.data.remote.PlaceOrderRequestDto
import com.example.data.remote.QuoteRequestDto
import com.example.data.remote.RedeemPromoRequestDto
import com.example.domain.error.DomainError
import com.example.domain.error.Outcome
import com.example.domain.error.map
import com.example.domain.model.Address
import com.example.domain.model.AddressValidation
import com.example.domain.model.Cart
import com.example.domain.model.Order
import com.example.domain.model.OrderRequest
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption
import com.example.domain.repository.AddressRepository
import com.example.domain.repository.CartReader
import com.example.domain.repository.CartWriter
import com.example.domain.repository.OrderRepository
import com.example.domain.repository.PaymentMethodRepository
import com.example.domain.repository.PromoRepository
import com.example.domain.repository.ShippingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Currency
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Implementations are `internal`: nothing outside :data can reference them.
// Hilt binds them to the domain ports in DataModule.

@Singleton
internal class CartRepositoryImpl @Inject constructor(
    private val dao: CartDao,
    @Named("storeCurrency") private val storeCurrency: Currency,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CartReader, CartWriter {

    override fun observeCart(): Flow<Cart> =
        dao.observeLines().map { it.toDomain(storeCurrency) }.flowOn(io)

    override suspend fun setQuantity(sku: String, quantity: Int): Outcome<Unit> = withContext(io) {
        if (dao.updateQuantity(sku, quantity) == 1) Outcome.Success(Unit) else Outcome.Failure(DomainError.NotFound)
    }

    override suspend fun removeLine(sku: String): Outcome<Unit> = withContext(io) {
        dao.delete(sku)
        Outcome.Success(Unit)
    }
}

internal class AddressRepositoryImpl @Inject constructor(
    private val api: CheckoutApi,
) : AddressRepository {

    // Simplified: a production app would cache in Room and refresh, like the cart.
    // Remote streams emit Outcome so a failed fetch is a value, never an exception.
    override fun observeAddresses(): Flow<Outcome<List<Address>>> = flow {
        emit(safeCall { api.addresses().map { it.toDomain() } })
    }

    override suspend fun validate(addressId: String): Outcome<AddressValidation> =
        safeCall { api.validateAddress(addressId).toDomain() }
}

internal class ShippingRepositoryImpl @Inject constructor(
    private val api: CheckoutApi,
) : ShippingRepository {

    override suspend fun quote(addressId: String, cart: Cart): Outcome<List<ShippingOption>> =
        safeCall { api.shippingQuotes(QuoteRequestDto(addressId, cart.toQuoteLines())).map { it.toDomain() } }
}

internal class PromoRepositoryImpl @Inject constructor(
    private val api: CheckoutApi,
) : PromoRepository {

    override suspend fun redeem(code: String, cart: Cart): Outcome<Promo> =
        safeCall(
            mapHttp = { e ->
                when (e.code()) {
                    404 -> DomainError.Promo.Invalid
                    410 -> DomainError.Promo.Expired
                    422 -> DomainError.Promo.MinimumNotReached
                    else -> null
                }
            },
        ) { api.redeemPromo(RedeemPromoRequestDto(code, cart.toQuoteLines())).toDomain() }
}

internal class PaymentMethodRepositoryImpl @Inject constructor(
    private val api: CheckoutApi,
) : PaymentMethodRepository {

    override fun observePaymentMethods(): Flow<Outcome<List<PaymentMethod>>> = flow {
        emit(safeCall { api.paymentMethods().map { it.toDomain() } })
    }
}

internal class OrderRepositoryImpl @Inject constructor(
    private val api: CheckoutApi,
) : OrderRepository {

    override suspend fun place(request: OrderRequest): Outcome<Order> {
        // Same key for retries of the same logical request, so a timeout + retry can't double-charge.
        val idempotencyKey = UUID.nameUUIDFromBytes(request.toString().toByteArray()).toString()
        return safeCall(
            mapHttp = { e ->
                when (e.code()) {
                    402 -> DomainError.Payment.Declined
                    403 -> DomainError.Payment.RequiresAction
                    422 -> DomainError.OutOfStock
                    else -> null
                }
            },
        ) {
            api.placeOrder(
                idempotencyKey,
                PlaceOrderRequestDto(request.addressId, request.shippingOptionId, request.paymentMethodId, request.promoCode),
            )
        }.map { Order(it.id) }
    }
}
