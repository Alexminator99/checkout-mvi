package com.example.core.ui

import com.example.core.domain.Money

/** How amounts are shown. Projectors depend on this, never on Locale or NumberFormat directly. */
fun interface MoneyFormatter {
    fun format(money: Money): String
}
