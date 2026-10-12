package com.example.feature.checkout.data.error

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.CheckoutApiException
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * The single place where exceptions become [CheckoutError]s.
 *
 * @param mapApiError the meaning of a status code for one endpoint (for example 410 on redeem means
 * the promo expired). When it returns null, [toCheckoutError] decides.
 */
internal suspend inline fun <T> safeCall(
    noinline mapApiError: (CheckoutApiException) -> CheckoutError? = { null },
    crossinline call: suspend () -> T,
): CheckoutOutcome<T> = try {
    Outcome.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: CheckoutApiException) {
    Outcome.Failure(mapApiError(e) ?: e.toCheckoutError())
} catch (e: IOException) {
    Outcome.Failure(CheckoutError.Network)
}

/** What a status code means when the endpoint gives it no specific meaning. */
internal fun CheckoutApiException.toCheckoutError(): CheckoutError = when (code) {
    HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> CheckoutError.Unauthorized
    HttpStatus.NOT_FOUND -> CheckoutError.NotFound
    HttpStatus.CONFLICT -> CheckoutError.PriceChanged
    else -> CheckoutError.Unexpected("HTTP $code")
}
