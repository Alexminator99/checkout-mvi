package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import javax.inject.Inject

class ResetDemoUseCase @Inject constructor(private val cartRepository: CartRepository) {
    suspend operator fun invoke(): CheckoutOutcome<Cart> = cartRepository.restoreDemoCart()
}
