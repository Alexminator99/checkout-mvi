package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository
import javax.inject.Inject

class RedeemPromoUseCase @Inject constructor(private val promoRepository: PromoRepository) {

    /** Promo codes are case-insensitive and ignore surrounding spaces. */
    suspend operator fun invoke(rawCode: String, cart: Cart): CheckoutOutcome<Promo> =
        promoRepository.redeem(rawCode.trim().uppercase(), cart)
}
