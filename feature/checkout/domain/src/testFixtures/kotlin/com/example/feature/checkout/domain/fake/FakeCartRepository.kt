package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCartRepository(initial: Cart = cartOf(MUG)) : CartRepository {

    val cart = MutableStateFlow(initial)

    /** When set, every write fails with this error and leaves the cart untouched. */
    var failure: CheckoutError? = null

    override fun observeCart(): Flow<Cart> = cart

    override suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit> = write {
        cart.update { current ->
            current.copy(lines = current.lines.map { if (it.sku == sku) it.copy(quantity = quantity) else it })
        }
    }

    override suspend fun removeLine(sku: String): CheckoutOutcome<Unit> = write {
        cart.update { current -> current.copy(lines = current.lines.filterNot { it.sku == sku }) }
    }

    private inline fun write(change: () -> Unit): CheckoutOutcome<Unit> {
        failure?.let { return Outcome.Failure(it) }
        change()
        return Outcome.Success(Unit)
    }
}
