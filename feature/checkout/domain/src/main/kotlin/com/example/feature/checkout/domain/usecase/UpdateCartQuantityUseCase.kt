package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import javax.inject.Inject

class UpdateCartQuantityUseCase @Inject constructor(private val cartRepository: CartRepository) {

    /** A quantity of zero or less removes the line; anything above the per-line maximum is capped. */
    suspend operator fun invoke(sku: String, quantity: Int): CheckoutOutcome<Unit> =
        if (quantity <= 0) cartRepository.removeLine(sku)
        else cartRepository.setQuantity(sku, quantity.coerceAtMost(MAX_QUANTITY_PER_LINE))
}
