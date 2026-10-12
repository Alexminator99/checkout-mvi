package com.example.feature.checkout.data.demo

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto

/** The demo backend's fixed data. Ids are stable so scripted scenarios can refer to them. */
internal object DemoCheckoutData {
    const val CURRENCY = "EUR"

    const val HOME_ID = "home"
    const val OFFICE_ID = "office"
    const val PO_BOX_ID = "po-box"
    const val ISLAND_ID = "island"

    const val VISA_ID = "visa"
    const val DECLINED_CARD_ID = "mastercard"
    const val CONFIRMATION_CARD_ID = "amex"

    val addresses: List<AddressDto> = listOf(
        AddressDto(HOME_ID, "Home", "Alex Doe", "Calle Larios 1", "Málaga", "29005", isDefault = true),
        AddressDto(OFFICE_ID, "Office", "Alex Doe", "Av. de Andalucía 10", "Málaga", "29007", isDefault = false),
        AddressDto(PO_BOX_ID, "PO Box", "Alex Doe", "PO Box 42", "Málaga", "29080", isDefault = false),
        AddressDto(ISLAND_ID, "Island cabin", "Alex Doe", "Isla de Tabarca 3", "Alicante", "03138", isDefault = false),
    )

    val shippingOptions: List<ShippingOptionDto> = listOf(
        ShippingOptionDto("standard", "Standard", priceMinor = 499, CURRENCY, etaDays = 3, express = false),
        ShippingOptionDto("pickup", "Pickup point", priceMinor = 299, CURRENCY, etaDays = 4, express = false),
        ShippingOptionDto("express", "Express", priceMinor = 1299, CURRENCY, etaDays = 1, express = true),
    )

    val paymentMethods: List<PaymentMethodDto> = listOf(
        PaymentMethodDto(VISA_ID, "Visa", "4242", isDefault = true),
        PaymentMethodDto(DECLINED_CARD_ID, "Mastercard", "0002", isDefault = false),
        PaymentMethodDto(CONFIRMATION_CARD_ID, "Amex", "3155", isDefault = false),
    )
}
