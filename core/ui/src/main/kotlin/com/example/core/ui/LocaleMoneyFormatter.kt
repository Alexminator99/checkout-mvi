package com.example.core.ui

import com.example.core.domain.Money
import java.text.NumberFormat
import java.util.Locale

class LocaleMoneyFormatter(private val locale: () -> Locale = Locale::getDefault) : MoneyFormatter {

    override fun format(money: Money): String =
        NumberFormat.getCurrencyInstance(locale())
            .apply { currency = money.currency }
            .format(money.amount)
}
