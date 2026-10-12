package com.example.feature.checkout.data.remote

import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * An in-memory backend with simulated latency and scripted outcomes, so the app runs end to end
 * without a server.
 *
 * - Promo codes: SAVE5 takes €5 off; BIG50 takes €50 off orders of at least €100; EXPIRED has
 *   expired; any other code is invalid.
 * - Payment: the Mastercard is declined and the Amex needs bank confirmation.
 * - Addresses: the PO box and the island cabin cannot be delivered to.
 * - Orders: placing twice with the same idempotency key returns the same order.
 */
internal class DemoCheckoutRemoteDataSource(private val latencyMillis: Long) : CheckoutRemoteDataSource {

    private val placedOrders = ConcurrentHashMap<String, OrderDto>()
    private val nextOrderNumber = AtomicInteger(FIRST_ORDER_NUMBER)

    override suspend fun addresses(): List<AddressDto> = respond { DemoCheckoutData.addresses }

    override suspend fun validateAddress(addressId: String): AddressValidationDto = respond {
        when (addressId) {
            DemoCheckoutData.HOME_ID, DemoCheckoutData.OFFICE_ID ->
                AddressValidationDto(deliverable = true, reasonCode = null)
            DemoCheckoutData.PO_BOX_ID -> AddressValidationDto(deliverable = false, reasonCode = "PO_BOX")
            DemoCheckoutData.ISLAND_ID -> AddressValidationDto(deliverable = false, reasonCode = "OUTSIDE_AREA")
            else -> throw CheckoutApiException(HttpStatus.NOT_FOUND)
        }
    }

    override suspend fun shippingQuotes(request: QuoteRequestDto): List<ShippingOptionDto> = respond {
        if (request.lines.isEmpty()) emptyList() else DemoCheckoutData.shippingOptions
    }

    override suspend fun redeemPromo(request: RedeemPromoRequestDto): PromoDto = respond {
        val subtotalMinor = request.lines.sumOf { it.unitPriceMinor * it.quantity }
        when (request.code) {
            "SAVE5" -> PromoDto(code = "SAVE5", discountMinor = 500, currency = DemoCheckoutData.CURRENCY)
            "BIG50" ->
                if (subtotalMinor >= BIG50_MINIMUM_MINOR) {
                    PromoDto(code = "BIG50", discountMinor = 5_000, currency = DemoCheckoutData.CURRENCY)
                } else {
                    throw CheckoutApiException(HttpStatus.UNPROCESSABLE_ENTITY)
                }
            "EXPIRED" -> throw CheckoutApiException(HttpStatus.GONE)
            else -> throw CheckoutApiException(HttpStatus.NOT_FOUND)
        }
    }

    override suspend fun paymentMethods(): List<PaymentMethodDto> = respond { DemoCheckoutData.paymentMethods }

    override suspend fun placeOrder(idempotencyKey: String, request: PlaceOrderRequestDto): OrderDto = respond {
        when (request.paymentMethodId) {
            DemoCheckoutData.DECLINED_CARD_ID -> throw CheckoutApiException(HttpStatus.PAYMENT_REQUIRED)
            DemoCheckoutData.CONFIRMATION_CARD_ID -> throw CheckoutApiException(HttpStatus.FORBIDDEN)
            else -> placedOrders.getOrPut(idempotencyKey) { OrderDto(id = "ORD-${nextOrderNumber.getAndIncrement()}") }
        }
    }

    private suspend fun <T> respond(response: () -> T): T {
        delay(latencyMillis)
        return response()
    }

    private companion object {
        const val FIRST_ORDER_NUMBER = 1001
        const val BIG50_MINIMUM_MINOR = 10_000L
    }
}
