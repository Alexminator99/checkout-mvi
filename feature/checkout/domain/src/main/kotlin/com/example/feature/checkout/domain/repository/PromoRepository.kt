package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo

interface PromoRepository {
    suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo>
}
