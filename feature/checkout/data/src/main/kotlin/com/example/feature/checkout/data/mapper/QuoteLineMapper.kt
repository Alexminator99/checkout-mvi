package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.domain.model.Cart

internal fun Cart.toQuoteLines(): List<QuoteLineDto> = lines.map { line ->
    QuoteLineDto(line.sku, line.quantity, line.unitPrice.minorUnits, line.unitPrice.currency.currencyCode)
}
