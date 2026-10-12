package com.example.data.mapper

import com.example.data.local.CartLineEntity
import com.example.data.remote.AddressDto
import com.example.data.remote.AddressValidationDto
import com.example.data.remote.PaymentMethodDto
import com.example.data.remote.PromoDto
import com.example.data.remote.QuoteLineDto
import com.example.data.remote.ShippingOptionDto
import com.example.domain.model.Address
import com.example.domain.model.AddressValidation
import com.example.domain.model.Cart
import com.example.domain.model.CartLine
import com.example.domain.model.Money
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption
import java.math.BigDecimal
import java.util.Currency

internal fun moneyFromMinor(minor: Long, currencyCode: String): Money {
    val currency = Currency.getInstance(currencyCode)
    return Money(BigDecimal.valueOf(minor, currency.defaultFractionDigits), currency)
}

internal fun List<CartLineEntity>.toDomain(fallbackCurrency: Currency): Cart = Cart(
    lines = map { CartLine(it.sku, it.name, moneyFromMinor(it.unitPriceMinor, it.currency), it.quantity) },
    currency = firstOrNull()?.let { Currency.getInstance(it.currency) } ?: fallbackCurrency,
)

internal fun AddressDto.toDomain() = Address(id, recipient, line1, city, postalCode, isDefault)

internal fun AddressValidationDto.toDomain(): AddressValidation =
    if (deliverable) AddressValidation.Deliverable
    else AddressValidation.Undeliverable(
        when (reasonCode) {
            "PO_BOX" -> AddressValidation.Reason.PO_BOX_NOT_ALLOWED
            "INCOMPLETE" -> AddressValidation.Reason.INCOMPLETE
            else -> AddressValidation.Reason.OUTSIDE_SERVICE_AREA
        }
    )

internal fun ShippingOptionDto.toDomain() =
    ShippingOption(id, label, moneyFromMinor(priceMinor, currency), etaDays, express)

internal fun PromoDto.toDomain() = Promo(code, moneyFromMinor(discountMinor, currency))

internal fun PaymentMethodDto.toDomain() = PaymentMethod(id, brand, last4, isDefault)

internal fun Cart.toQuoteLines() = lines.map { QuoteLineDto(it.sku, it.quantity) }
