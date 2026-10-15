package com.example.feature.checkout.data.remote

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto

/**
 * The checkout backend as the app sees it. Implementations report error responses as
 * [CheckoutApiException] and connectivity failures as [java.io.IOException].
 */
internal interface CheckoutRemoteDataSource {
    suspend fun refreshCart(lines: List<QuoteLineDto>): List<QuoteLineDto>
    suspend fun addresses(): List<AddressDto>
    suspend fun validateAddress(addressId: String): AddressValidationDto
    suspend fun shippingQuotes(request: QuoteRequestDto): List<ShippingOptionDto>
    suspend fun redeemPromo(request: RedeemPromoRequestDto): PromoDto
    suspend fun paymentMethods(): List<PaymentMethodDto>
    suspend fun placeOrder(idempotencyKey: String, request: PlaceOrderRequestDto): OrderDto
}
