package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository

class FakePromoRepository(var result: CheckoutOutcome<Promo> = Outcome.Success(SAVE5)) : PromoRepository {

    val redeemedCodes = mutableListOf<String>()

    override suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo> {
        redeemedCodes += code
        return result
    }
}
