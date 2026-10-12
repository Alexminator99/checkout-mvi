package com.example.feature.checkout.data.remote

/** A failed backend call, carrying the status code the backend answered with. */
internal class CheckoutApiException(val code: Int) : Exception("HTTP $code")
