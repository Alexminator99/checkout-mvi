package com.example.domain.usecase

import com.example.domain.model.Cart
import com.example.domain.model.CartLine
import com.example.domain.model.Customer
import com.example.domain.model.Money
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Currency

class CalculateOrderTotalsUseCaseTest {

    private val eur = Currency.getInstance("EUR")
    private fun m(v: String) = Money.of(v, "EUR")
    private val cart = Cart(listOf(CartLine("a", "A", m("10.00"), 2)), eur)
    private val standard = ShippingOption("std", "Standard", m("4.99"), 3, isExpress = false)
    private val express = ShippingOption("exp", "Express", m("12.99"), 1, isExpress = true)
    private val regular = Customer("c", "C", isPremium = false)
    private val member = Customer("c", "C", isPremium = true)
    private val calculate = CalculateOrderTotalsUseCase()

    @Test
    fun `regular customer pays shipping`() {
        assertEquals(m("24.99"), calculate(cart, standard, null, regular).total)
    }

    @Test
    fun `members get standard shipping free but not express`() {
        assertEquals(m("20.00"), calculate(cart, standard, null, member).total)
        assertEquals(m("32.99"), calculate(cart, express, null, member).total)
    }

    @Test
    fun `discount never exceeds the subtotal`() {
        val huge = Promo("ALL", m("500.00"))
        assertEquals(m("4.99"), calculate(cart, standard, huge, regular).total)
    }
}
