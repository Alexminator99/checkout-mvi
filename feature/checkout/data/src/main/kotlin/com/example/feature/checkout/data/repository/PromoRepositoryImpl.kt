package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toQuoteLines
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.data.remote.dto.RedeemPromoRequestDto
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.repository.PromoRepository
import javax.inject.Inject

internal class PromoRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : PromoRepository {

    override suspend fun redeem(code: String, cart: Cart): CheckoutOutcome<Promo> = safeCall(
        mapApiError = { error ->
            when (error.code) {
                HttpStatus.NOT_FOUND -> CheckoutError.Promo.Invalid
                HttpStatus.GONE -> CheckoutError.Promo.Expired
                HttpStatus.UNPROCESSABLE_ENTITY -> CheckoutError.Promo.MinimumNotReached
                else -> null
            }
        },
    ) {
        remote.redeemPromo(RedeemPromoRequestDto(code, cart.toQuoteLines())).toDomain()
    }
}
