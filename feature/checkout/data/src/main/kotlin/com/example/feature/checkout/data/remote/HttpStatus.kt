package com.example.feature.checkout.data.remote

/** The status codes the checkout backend uses. */
internal object HttpStatus {
    const val UNAUTHORIZED = 401
    const val PAYMENT_REQUIRED = 402
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val CONFLICT = 409
    const val GONE = 410
    const val UNPROCESSABLE_ENTITY = 422
}
