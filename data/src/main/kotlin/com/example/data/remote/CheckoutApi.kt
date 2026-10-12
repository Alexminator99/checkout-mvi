package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** Retrofit contract. DTOs never leave :data; mappers turn them into domain models. */
interface CheckoutApi {

    @GET("me/addresses")
    suspend fun addresses(): List<AddressDto>

    @POST("addresses/{id}/validate")
    suspend fun validateAddress(@Path("id") addressId: String): AddressValidationDto

    @POST("shipping/quotes")
    suspend fun shippingQuotes(@Body body: QuoteRequestDto): List<ShippingOptionDto>

    @POST("promos/redeem")
    suspend fun redeemPromo(@Body body: RedeemPromoRequestDto): PromoDto

    @GET("me/payment-methods")
    suspend fun paymentMethods(): List<PaymentMethodDto>

    @POST("orders")
    suspend fun placeOrder(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PlaceOrderRequestDto,
    ): OrderDto
}

// --- DTOs (serialization annotations omitted for brevity; use kotlinx.serialization or Moshi) ---

data class AddressDto(val id: String, val recipient: String, val line1: String, val city: String, val postalCode: String, val isDefault: Boolean)
data class AddressValidationDto(val deliverable: Boolean, val reasonCode: String?)
data class QuoteRequestDto(val addressId: String, val lines: List<QuoteLineDto>)
data class QuoteLineDto(val sku: String, val quantity: Int)
data class ShippingOptionDto(val id: String, val label: String, val priceMinor: Long, val currency: String, val etaDays: Int, val express: Boolean)
data class RedeemPromoRequestDto(val code: String, val lines: List<QuoteLineDto>)
data class PromoDto(val code: String, val discountMinor: Long, val currency: String)
data class PaymentMethodDto(val id: String, val brand: String, val last4: String, val isDefault: Boolean)
data class PlaceOrderRequestDto(val addressId: String, val shippingOptionId: String, val paymentMethodId: String, val promoCode: String?)
data class OrderDto(val id: String)
data class ApiErrorDto(val code: String)
