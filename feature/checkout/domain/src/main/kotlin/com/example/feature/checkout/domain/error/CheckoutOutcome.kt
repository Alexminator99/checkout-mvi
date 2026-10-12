package com.example.feature.checkout.domain.error

import com.example.core.domain.Outcome

typealias CheckoutOutcome<T> = Outcome<T, CheckoutError>
