package com.example.core.ui

import com.example.domain.model.Money
import java.text.NumberFormat
import java.util.Locale

/** Presentation port: projectors depend on this, never on Locale or NumberFormat directly. */
fun interface MoneyFormatter {
    fun format(money: Money): String
}

class LocaleMoneyFormatter(private val locale: () -> Locale = Locale::getDefault) : MoneyFormatter {
    override fun format(money: Money): String =
        NumberFormat.getCurrencyInstance(locale()).apply { currency = money.currency }.format(money.amount)
}
