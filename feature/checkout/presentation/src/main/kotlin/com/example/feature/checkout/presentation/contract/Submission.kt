package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

internal sealed interface Submission {
    data object Idle : Submission
    data object Submitting : Submission
    data object Completed : Submission
    data class Failed(val error: CheckoutError) : Submission
}
